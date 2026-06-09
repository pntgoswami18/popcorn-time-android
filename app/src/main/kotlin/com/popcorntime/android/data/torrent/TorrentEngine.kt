package com.popcorntime.android.data.torrent

import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.domain.model.Torrent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.libtorrent4j.AlertListener
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.swig.settings_pack
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.alerts.PieceFinishedAlert
import org.libtorrent4j.alerts.TorrentErrorAlert
import org.libtorrent4j.swig.torrent_flags_t
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TorrentEngine wraps libtorrent4j's SessionManager and provides a coroutine-friendly
 * StateFlow of [StreamState]. The stream server ([TorrentStreamServer]) serves the
 * largest video file to ExoPlayer over localhost HTTP once enough of the file is buffered.
 *
 * Flow mirrors popcorn-desktop's streamer.js logic:
 *   magnet/url → metadata → sequential download → buffer → ExoPlayer-ready
 *
 * Thread-safety note
 * ──────────────────
 * [currentHandle] is accessed from two contexts:
 *   • The monitor coroutine (Dispatchers.IO)
 *   • The libtorrent alert thread (AlertListener.alert)
 *   • The main thread (stopCurrent / startStream)
 *
 * The guard strategy is:
 *   1. In stopCurrent(), null [currentHandle] BEFORE calling session.remove() so the
 *      monitor loop sees null the next time it checks, even if it is currently mid-loop.
 *   2. Check handle.isValid before every libtorrent call.
 *   3. Wrap every handle call in runCatching so a race-window INVALID_TORRENT_HANDLE
 *      (libtorrent error 20) surfaces as a warning, not a crash.
 */
@Singleton
class TorrentEngine @Inject constructor(
    private val cacheDir: File,
    private val streamServer: TorrentStreamServer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = SessionManager()

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    val state: StateFlow<StreamState> = _state.asStateFlow()

    private val _downloadedBytes = MutableStateFlow(0L)
    private val _totalBytes = MutableStateFlow(0L)
    val downloadedBytes: StateFlow<Long> = _downloadedBytes.asStateFlow()
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    private val currentHandleRef = java.util.concurrent.atomic.AtomicReference<TorrentHandle?>(null)
    private var monitorJob: kotlinx.coroutines.Job? = null

    // 0.5 % lets ExoPlayer begin decoding quickly; sequential mode ensures in-order delivery.
    private val BUFFER_THRESHOLD = 0.005f

    init { startSession() }

    private fun startSession() {
        session.start()
        session.addListener(torrentListener)
        Timber.d("TorrentEngine: session started")
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun startStream(torrent: Torrent, saveDir: File = cacheDir) {
        stopCurrent()
        _state.value = StreamState.Buffering(0f, 0L, 0L, 0, 0)

        monitorJob = scope.launch {
            try {
                val uri = torrent.magnet.ifBlank { torrent.url }
                session.download(uri, saveDir, torrent_flags_t.from_int(0))
                monitorProgress()
            } catch (e: Exception) {
                Timber.e(e, "TorrentEngine: failed to start stream")
                _state.value = StreamState.Error(e.message ?: "Unknown torrent error")
            }
        }
    }

    fun setError(message: String) {
        _state.value = StreamState.Error(message)
    }

    /**
     * Stops the current torrent cleanly.
     *
     * Order matters:
     *  1. Cancel the monitor coroutine first (cooperative — it will stop at next delay).
     *  2. Null [currentHandle] BEFORE session.remove() so any in-flight loop iteration
     *     that resumes after its delay finds null and exits cleanly.
     *  3. Remove the torrent from the session.
     */
    fun stopCurrent() {
        monitorJob?.cancel()
        monitorJob = null
        val handle = currentHandleRef.getAndSet(null) // null BEFORE session.remove()
        handle?.let { runCatching { session.remove(it) } }
        streamServer.stop()
        _state.value = StreamState.Idle
    }

    fun applySpeedLimits(downloadKbps: Int, uploadKbps: Int) {
        scope.launch {
            runCatching {
                val sp = session.settings()
                sp.setInteger(
                    settings_pack.int_types.download_rate_limit.swigValue(),
                    if (downloadKbps > 0) downloadKbps * 1024 else 0,
                )
                sp.setInteger(
                    settings_pack.int_types.upload_rate_limit.swigValue(),
                    if (uploadKbps > 0) uploadKbps * 1024 else 0,
                )
                session.applySettings(sp)
            }
        }
    }

    fun release() {
        stopCurrent()
        session.removeListener(torrentListener)
        session.stop()
        scope.cancel()
    }

    /** Returns the absolute path of the largest video file in the current torrent, or null. */
    fun getVideoFilePath(): String? {
        val handle = currentHandleRef.get() ?: return null
        return findVideoFile(handle)?.absolutePath
    }

    // ── Internal monitor loop ─────────────────────────────────────────────────

    private suspend fun monitorProgress() {
        while (coroutineContext.isActive) {
            val handle = resolveHandle()

            if (handle == null || !safeIsValid(handle)) {
                // No torrent yet, or handle was just invalidated — wait and retry.
                currentHandleRef.set(null)
                delay(500)
                continue
            }

            // Only refresh if we haven't been stopped
            if (currentHandleRef.get() != null) {
                currentHandleRef.set(handle)
            }

            // Wrap every handle call: a race between stopCurrent() and this loop
            // can still produce an INVALID_TORRENT_HANDLE even after isValid() == true.
            val status = runCatching { handle.status() }.getOrNull()
            if (status == null) {
                Timber.w("TorrentEngine: handle invalidated during status poll, retrying")
                currentHandleRef.set(null)
                delay(500)
                continue
            }

            val progress = status.progress()
            val dlSpeed  = status.downloadRate().toLong()
            val ulSpeed  = status.uploadRate().toLong()
            val seeds    = status.numSeeds()
            val peers    = status.numPeers()
            _downloadedBytes.value = runCatching { status.totalWantedDone() }.getOrDefault(0L)
            _totalBytes.value = runCatching { status.totalWanted() }.getOrDefault(0L)

            when (_state.value) {
                is StreamState.Buffering -> {
                    _state.value = StreamState.Buffering(progress, dlSpeed, ulSpeed, seeds, peers)
                    if (progress >= BUFFER_THRESHOLD) {
                        val videoFile = findVideoFile(handle)
                        if (videoFile == null) { delay(1000); continue }
                        val streamUrl = streamServer.start(videoFile)
                        _state.value = StreamState.Ready(streamUrl, dlSpeed, ulSpeed, seeds, peers, progress)
                    }
                }
                is StreamState.Ready -> {
                    _state.value = (_state.value as StreamState.Ready).copy(
                        downloadSpeed = dlSpeed,
                        uploadSpeed   = ulSpeed,
                        seeds         = seeds,
                        peers         = peers,
                        progress      = progress,
                    )
                }
                else -> {}
            }
            delay(1000)
        }
    }

    /** Returns the cached handle, or the first one from the swig session list. */
    private fun resolveHandle(): TorrentHandle? {
        currentHandleRef.get()?.let { return it }
        return runCatching {
            val list = session.swig().get_torrents()
            if (list.size > 0) TorrentHandle(list.get(0)) else null
        }.getOrNull()
    }

    /** isValid() itself can throw if the internal pointer is already freed. */
    private fun safeIsValid(handle: TorrentHandle): Boolean =
        runCatching { handle.isValid }.getOrDefault(false)

    private fun findVideoFile(handle: TorrentHandle): File? {
        if (!safeIsValid(handle)) return null
        val info = runCatching { handle.torrentFile() }.getOrNull() ?: return null
        var largestIndex = -1
        var largestSize  = 0L
        for (i in 0 until info.numFiles()) {
            val size = info.files().fileSize(i)
            if (size > largestSize) { largestSize = size; largestIndex = i }
        }
        if (largestIndex < 0) return null
        val priorities = Array(info.numFiles()) {
            if (it == largestIndex) Priority.DEFAULT else Priority.IGNORE
        }
        runCatching { handle.prioritizeFiles(priorities) }
        runCatching { handle.setSequentialRange(0, info.numPieces() - 1) }
        val path = runCatching { handle.savePath() }.getOrNull() ?: return null
        return File(path + "/" + info.files().filePath(largestIndex))
    }

    // ── Alert listener ────────────────────────────────────────────────────────

    private val torrentListener = object : AlertListener {
        override fun types() = intArrayOf(
            AlertType.METADATA_RECEIVED.swig(),
            AlertType.PIECE_FINISHED.swig(),
            AlertType.TORRENT_ERROR.swig(),
        )

        override fun alert(alert: Alert<*>) {
            when (alert) {
                is MetadataReceivedAlert -> {
                    val h = alert.handle()
                    if (safeIsValid(h)) {
                        currentHandleRef.compareAndSet(null, h)
                        runCatching { val inf = h.torrentFile(); if (inf != null) h.setSequentialRange(0, inf.numPieces() - 1) }
                        Timber.d("TorrentEngine: metadata received — ${runCatching { h.name }.getOrDefault("?")}")
                    }
                }
                is PieceFinishedAlert -> { /* progress polled in monitorProgress */ }
                is TorrentErrorAlert -> {
                    val msg = alert.error().message
                    Timber.e("TorrentEngine: torrent error — $msg")
                    _state.value = StreamState.Error(msg ?: "Unknown torrent error")
                }
            }
        }
    }
}
