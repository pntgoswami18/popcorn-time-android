package com.popcorntime.android.data.torrent

import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.domain.model.Torrent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.libtorrent4j.AlertListener
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.alerts.PieceFinishedAlert
import org.libtorrent4j.alerts.TorrentErrorAlert
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

    // Minimum buffered percentage before declaring Ready (mirrors desktop's readiness check)
    private val BUFFER_THRESHOLD = 0.03f  // 3% of file

    init { startSession() }

    private fun startSession() {
        val settings = SettingsPack().apply {
            setString(SettingsPack.string_types.listen_interfaces.swigValue(), "0.0.0.0:6881")
            setBoolean(SettingsPack.bool_types.enable_dht.swigValue(), true)
            setBoolean(SettingsPack.bool_types.enable_lsd.swigValue(), true)
            setBoolean(SettingsPack.bool_types.enable_upnp.swigValue(), true)
            setBoolean(SettingsPack.bool_types.enable_natpmp.swigValue(), true)
        }
        session.start(SessionParams(settings))
        session.addListener(torrentListener)
        Timber.d("TorrentEngine: session started")
    }

    fun startStream(torrent: Torrent, saveDir: File = cacheDir) {
        stopCurrent()
        _state.value = StreamState.Buffering(0f, 0L, 0L, 0, 0)

        scope.launch {
            try {
                val uri = torrent.magnet.ifBlank { torrent.url }
                session.download(uri, saveDir)
                monitorProgress()
            } catch (e: Exception) {
                Timber.e(e, "Failed to start torrent stream")
                _state.value = StreamState.Error(e.message ?: "Unknown torrent error")
            }
        }
    }

    private suspend fun monitorProgress() {
        while (scope.isActive) {
            val handle = session.torrents().firstOrNull() ?: run {
                delay(500)
                return@monitorProgress
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
                        val videoFile = findVideoFile(handle) ?: run {
                            delay(1000)
                            return@monitorProgress
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
        // Set sequential download + prioritise from the start
        val priorities = Array(info.numFiles()) { if (it == largestIndex) Priority.NORMAL else Priority.IGNORE }
        handle.prioritizeFiles(priorities)
        handle.setSequentialDownload(true)
        val path = handle.savePath() + "/" + info.files().filePath(largestIndex)
        return File(path)
    }

    fun stopCurrent() {
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
                is MetadataReceivedAlert -> Timber.d("Metadata received: ${alert.handle().name()}")
                is PieceFinishedAlert -> { /* progress is polled in monitorProgress */ }
                is TorrentErrorAlert -> {
                    val msg = alert.error().message()
                    Timber.e("Torrent error: $msg")
                    _state.value = StreamState.Error(msg)
                }
            }
        }
    }
}
