package com.popcorntime.android.data.torrent

import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.domain.model.Torrent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private var currentHandle: TorrentHandle? = null
    // Track the active monitor job so we can cancel it cleanly on stopCurrent()
    private var monitorJob: kotlinx.coroutines.Job? = null

    // Minimum buffered percentage before declaring Ready.
    // 0.5% lets ExoPlayer begin decoding quickly; sequential mode ensures in-order delivery.
    private val BUFFER_THRESHOLD = 0.005f  // 0.5% of torrent

    init { startSession() }

    private fun startSession() {
        // libtorrent4j 2.x: start() with no args uses built-in defaults.
        // string_types/bool_types nested enums were removed in 2.x SWIG bindings.
        session.start()
        session.addListener(torrentListener)
        Timber.d("TorrentEngine: session started")
    }

    fun startStream(torrent: Torrent, saveDir: File = cacheDir) {
        stopCurrent()   // cancels monitorJob and removes prior torrent
        _state.value = StreamState.Buffering(0f, 0L, 0L, 0, 0)

        monitorJob = scope.launch {
            try {
                val uri = torrent.magnet.ifBlank { torrent.url }
                // libtorrent4j 2.x: download(String, File) overload was removed;
                // the string/magnet overload now requires explicit flags.
                session.download(uri, saveDir, torrent_flags_t.from_int(0))
                monitorProgress()
            } catch (e: Exception) {
                Timber.e(e, "Failed to start torrent stream")
                _state.value = StreamState.Error(e.message ?: "Unknown torrent error")
            }
        }
    }

    private suspend fun monitorProgress() {
        // coroutineContext.isActive works inside a suspend fun; the bare `isActive`
        // extension is defined on CoroutineScope and isn't in scope here.
        while (coroutineContext.isActive) {
            // libtorrent4j 2.x: session has no torrents() method.
            // Use handle cached from TorrentAddedAlert, or fall back to swig list.
            val handle: TorrentHandle? = currentHandle ?: run {
                val swigList = session.swig().get_torrents()
                // swigList.size is a Kotlin property mapped from Java size()
                if (swigList.size > 0) TorrentHandle(swigList.get(0)) else null
            }
            if (handle == null) {
                delay(500)
                continue
            }
            currentHandle = handle
            val status = handle.status()
            val progress = status.progress()
            val dlSpeed = status.downloadRate().toLong()
            val ulSpeed = status.uploadRate().toLong()
            val seeds = status.numSeeds()
            val peers = status.numPeers()

            when (_state.value) {
                is StreamState.Buffering -> {
                    _state.value = StreamState.Buffering(progress, dlSpeed, ulSpeed, seeds, peers)
                    if (progress >= BUFFER_THRESHOLD) {
                        // Find the largest file in the torrent — that's the video
                        val videoFile = findVideoFile(handle)
                        if (videoFile == null) {
                            delay(1000)
                            continue
                        }
                        val streamUrl = streamServer.start(videoFile)
                        _state.value = StreamState.Ready(streamUrl, dlSpeed, ulSpeed, seeds, peers, progress)
                    }
                }
                is StreamState.Ready -> {
                    _state.value = (_state.value as StreamState.Ready).copy(
                        downloadSpeed = dlSpeed,
                        uploadSpeed = ulSpeed,
                        seeds = seeds,
                        peers = peers,
                        progress = progress,
                    )
                }
                else -> {}
            }
            delay(1000)
        }
    }

    private fun findVideoFile(handle: TorrentHandle): File? {
        val info = handle.torrentFile() ?: return null
        var largestIndex = -1
        var largestSize = 0L
        for (i in 0 until info.numFiles()) {
            val size = info.files().fileSize(i)
            if (size > largestSize) {
                largestSize = size
                largestIndex = i
            }
        }
        if (largestIndex < 0) return null
        // libtorrent4j 2.x Priority enum: IGNORE, LOW, TWO, THREE, DEFAULT, FIVE, SIX, TOP_PRIORITY
        val priorities = Array(info.numFiles()) { if (it == largestIndex) Priority.DEFAULT else Priority.IGNORE }
        handle.prioritizeFiles(priorities)
        // Enable sequential download: setSequentialRange(pieceIndex) sets sequential from that piece.
        // Calling with 0 enables sequential download from the beginning of the file.
        handle.setSequentialRange(0)
        val path = handle.savePath() + "/" + info.files().filePath(largestIndex)
        return File(path)
    }

    fun setError(message: String) {
        _state.value = StreamState.Error(message)
    }

    fun stopCurrent() {
        monitorJob?.cancel()   // stop the monitor loop before removing the torrent
        monitorJob = null
        currentHandle?.let { session.remove(it) }
        currentHandle = null
        streamServer.stop()
        _state.value = StreamState.Idle
    }

    fun release() {
        stopCurrent()
        session.removeListener(torrentListener)
        session.stop()
    }

    private val torrentListener = object : AlertListener {
        override fun types() = intArrayOf(
            AlertType.METADATA_RECEIVED.swig(),
            AlertType.PIECE_FINISHED.swig(),
            AlertType.TORRENT_ERROR.swig(),
        )

        override fun alert(alert: Alert<*>) {
            when (alert) {
                is MetadataReceivedAlert -> {
                    // Cache the handle and enable sequential download immediately so
                    // ExoPlayer can read the beginning of the file as pieces arrive.
                    val h = alert.handle()
                    currentHandle = h
                    try { h.setSequentialRange(0) } catch (_: Exception) { /* best-effort */ }
                    Timber.d("Metadata received: ${h.name}")
                }
                is PieceFinishedAlert -> { /* progress is polled in monitorProgress */ }
                is TorrentErrorAlert -> {
                    // libtorrent4j 2.x: ErrorCode.message is a String property, not a method.
                    val msg = alert.error().message
                    Timber.e("Torrent error: $msg")
                    _state.value = StreamState.Error(msg ?: "Unknown torrent error")
                }
            }
        }
    }
}
