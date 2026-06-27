package com.popcorntime.android.data.torrent

import android.content.Context
import com.popcorntime.android.data.db.dao.DownloadDao
import com.popcorntime.android.data.db.entity.DownloadEntity
import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.domain.model.Torrent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadStats(
    val imdbId: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val downloadSpeedBps: Long,
)

@Singleton
class DownloadManager @Inject constructor(
    private val torrentEngine: TorrentEngine,
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads: StateFlow<List<DownloadEntity>> = downloadDao.observeAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val activeDownloadImdbId: AtomicReference<String?> = AtomicReference(null)
    private val inFlightIds = Collections.synchronizedSet(mutableSetOf<String>())

    /** Set to the download's imdbId right before torrentEngine.startStream is invoked for it,
     *  cleared when its coroutine finishes. Gates cancelDownload's stopCurrent() so we never
     *  stop a torrent the engine is running for someone else (e.g. a stream). */
    private val engineStartedForImdbId: AtomicReference<String?> = AtomicReference(null)

    /** Observable mirror of [activeDownloadImdbId] so the UI can tell which incomplete
     *  entity is the actively-downloading one even before stats start arriving. */
    private val _activeImdbId = MutableStateFlow<String?>(null)
    val activeImdbId: StateFlow<String?> = _activeImdbId.asStateFlow()

    private val _activeDownloadStats = MutableStateFlow<DownloadStats?>(null)
    val activeDownloadStats: StateFlow<DownloadStats?> = _activeDownloadStats.asStateFlow()

    init {
        // Clean up any in-progress rows from a previous session
        scope.launch {
            downloadDao.deleteIncomplete()
        }
    }

    /**
     * Starts downloading [imdbId]. Only ONE download may run at a time: the torrent
     * engine has a single slot, so a second concurrent download would preempt (and
     * corrupt the bookkeeping of) the first.
     *
     * @return false if another download is already in progress (nothing was started);
     *   the caller should surface this to the user.
     */
    fun startDownload(imdbId: String, title: String, magnetUrl: String, quality: String = ""): Boolean {
        // Claim the single download slot synchronously so two rapid calls can't both start.
        synchronized(inFlightIds) {
            if (inFlightIds.isNotEmpty()) return false // a download is already in progress
            inFlightIds.add(imdbId)
        }
        scope.launch {
            if (downloads.value.any { it.imdbId == imdbId }) {
                inFlightIds.remove(imdbId)
                return@launch
            }
            try {
                // Set activeDownloadImdbId BEFORE any suspend call so that cancelDownload's
                // compareAndSet can always find it from the moment this try body begins.
                // The finally block always clears it regardless of how we exit.
                activeDownloadImdbId.set(imdbId)
                _activeImdbId.value = imdbId
                // Each download gets its own subdirectory so deleting one download can
                // never touch another download's files (or the shared downloads root).
                val saveDir = File(downloadsRootDir(), imdbId).also { it.mkdirs() }
                val entity = DownloadEntity(
                    imdbId = imdbId,
                    title = title,
                    magnetUrl = magnetUrl,
                    quality = quality,
                    filePath = saveDir.absolutePath,
                )
                downloadDao.insert(entity)
                // If cancelDownload fired concurrently it removed imdbId from inFlightIds
                // and cleared activeDownloadImdbId via CAS. Re-check both: if we're no
                // longer active or in-flight, abort.
                if (imdbId !in inFlightIds || activeDownloadImdbId.get() != imdbId) {
                    downloadDao.delete(imdbId)
                    return@launch
                }
                val torrent = Torrent(
                    url = magnetUrl,
                    magnet = magnetUrl,
                    quality = quality,
                    type = "download",
                    size = 0L,
                    fileSize = "",
                    seeds = 0,
                    peers = 0,
                    hash = "",
                )
                // Mark that the engine is (about to be) running THIS download, then
                // re-check cancellation one last time: if cancel fired in between, abort
                // before the engine starts.
                engineStartedForImdbId.set(imdbId)
                if (imdbId !in inFlightIds || activeDownloadImdbId.get() != imdbId) {
                    engineStartedForImdbId.compareAndSet(imdbId, null)
                    downloadDao.delete(imdbId)
                    return@launch
                }
                torrentEngine.startStream(torrent, saveDir)
                // Collect live stats while the torrent is downloading
                val statsJob = launch {
                    torrentEngine.state.collect { state ->
                        when (state) {
                            is StreamState.Buffering -> _activeDownloadStats.value = DownloadStats(
                                imdbId = imdbId,
                                progress = state.progress,
                                downloadedBytes = torrentEngine.downloadedBytes.value,
                                totalBytes = torrentEngine.totalBytes.value,
                                downloadSpeedBps = state.downloadSpeed,
                            )
                            is StreamState.Ready -> _activeDownloadStats.value = DownloadStats(
                                imdbId = imdbId,
                                progress = state.progress,
                                downloadedBytes = torrentEngine.downloadedBytes.value,
                                totalBytes = torrentEngine.totalBytes.value,
                                downloadSpeedBps = state.downloadSpeed,
                            )
                            else -> {}
                        }
                    }
                }
                // Wait for streaming-ready, error, or cancel (Idle).
                val readyOrError = torrentEngine.state.first {
                    it is StreamState.Ready || it is StreamState.Error || it is StreamState.Idle
                }
                if (readyOrError is StreamState.Error || readyOrError is StreamState.Idle) {
                    statsJob.cancel()
                    _activeDownloadStats.value = null
                    downloadDao.delete(imdbId)
                    activeDownloadImdbId.compareAndSet(imdbId, null)
                    return@launch
                }
                val finalState = torrentEngine.state.first { state ->
                    state is StreamState.Error || state is StreamState.Idle ||
                        (state is StreamState.Ready && isFullyDownloaded(state))
                }
                statsJob.cancel()
                _activeDownloadStats.value = null
                if (finalState is StreamState.Error) {
                    downloadDao.delete(imdbId)
                    activeDownloadImdbId.compareAndSet(imdbId, null)
                    return@launch
                }
                if (finalState is StreamState.Idle) {
                    downloadDao.delete(imdbId)
                    activeDownloadImdbId.compareAndSet(imdbId, null)
                    return@launch
                }
                if (finalState is StreamState.Ready) {
                    // Guard: only mark complete while we're still the active download.
                    // If we were cancelled/preempted, the engine's file path may belong
                    // to something else entirely.
                    if (activeDownloadImdbId.get() == imdbId) {
                        val videoFilePath = torrentEngine.getVideoFilePath() ?: saveDir.absolutePath
                        downloadDao.markComplete(imdbId, videoFilePath, System.currentTimeMillis())
                        activeDownloadImdbId.compareAndSet(imdbId, null)
                    } else {
                        // Cancelled while finishing — make sure no zombie row remains.
                        downloadDao.delete(imdbId)
                    }
                }
            } finally {
                // Always clean up, even if an uncaught exception escapes the try body
                // (e.g. torrentEngine.startStream throws). Without this, activeDownloadImdbId
                // would stay set permanently and startDownload would reject every future
                // download as "already in progress".
                _activeDownloadStats.value = null
                engineStartedForImdbId.compareAndSet(imdbId, null)
                activeDownloadImdbId.compareAndSet(imdbId, null)
                _activeImdbId.compareAndSet(imdbId, null)
                inFlightIds.remove(imdbId)
            }
        }
        return true
    }

    fun cancelDownload(imdbId: String) {
        // Removing from inFlightIds is the cancellation signal: startDownload re-checks
        // membership at its guard points (after the DB insert and right before
        // startStream) and aborts if the id is gone.
        inFlightIds.remove(imdbId)
        scope.launch {
            // compareAndSet atomically clears the active-ID only when it still matches,
            // preventing a TOCTOU race where another coroutine changes activeDownloadImdbId
            // between our read and the stopCurrent() call.
            if (activeDownloadImdbId.compareAndSet(imdbId, null)) {
                _activeImdbId.compareAndSet(imdbId, null)
                // Only stop the engine if startStream was actually invoked for THIS
                // download — otherwise we'd stop whatever else (e.g. a stream) owns
                // the single-slot engine.
                if (engineStartedForImdbId.compareAndSet(imdbId, null)) {
                    torrentEngine.stopCurrent()
                }
            }
            downloadDao.delete(imdbId)
        }
    }

    private fun isFullyDownloaded(state: StreamState.Ready): Boolean {
        if (state.progress >= 0.99f) return true
        val total = torrentEngine.totalBytes.value
        val downloaded = torrentEngine.downloadedBytes.value
        return total > 0L && downloaded >= total
    }

    fun deleteDownload(imdbId: String) {
        // Signal any in-progress download to abort before touching files.
        inFlightIds.remove(imdbId)
        scope.launch {
            // Stop the torrent engine if it is actively downloading this item.
            // Mirrors cancelDownload's CAS pattern so we never stop a stream
            // that belongs to a different imdbId.
            if (activeDownloadImdbId.compareAndSet(imdbId, null)) {
                _activeImdbId.compareAndSet(imdbId, null)
                if (engineStartedForImdbId.compareAndSet(imdbId, null)) {
                    torrentEngine.stopCurrent()
                }
            }
            val entity = downloads.value.find { it.imdbId == imdbId }
            val root = downloadsRootDir()
            // New layout: every download lives in its own <root>/<imdbId> subdirectory.
            safeDeleteUnder(root, File(root, imdbId))
            // Also honour the stored path (completed rows point at the video file; legacy
            // rows may point elsewhere). safeDeleteUnder refuses anything that is the
            // shared root itself or escapes it, so old rows that stored the shared root
            // only lose their DB row — never the other downloads.
            entity?.filePath?.let { safeDeleteUnder(root, File(it)) }
            downloadDao.delete(imdbId)
        }
    }

    private fun downloadsRootDir(): File =
        context.getExternalFilesDir("downloads") ?: context.filesDir

    /** Deletes [target] only if it resolves strictly under [root] (never root itself). */
    private fun safeDeleteUnder(root: File, target: File) {
        if (!target.exists()) return
        val rootCanonical = runCatching { root.canonicalFile }.getOrNull() ?: return
        val targetCanonical = runCatching { target.canonicalFile }.getOrNull() ?: return
        if (targetCanonical == rootCanonical ||
            !targetCanonical.path.startsWith(rootCanonical.path + File.separator)
        ) {
            Timber.w(
                "deleteDownload: refusing to delete %s — not strictly under downloads root %s",
                targetCanonical, rootCanonical,
            )
            return
        }
        if (targetCanonical.isDirectory) targetCanonical.deleteRecursively() else targetCanonical.delete()
    }
}
