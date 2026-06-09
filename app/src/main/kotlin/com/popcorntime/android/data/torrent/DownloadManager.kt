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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

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

    init {
        // Clean up any in-progress rows from a previous session
        scope.launch {
            downloadDao.deleteIncomplete()
        }
    }

    fun startDownload(imdbId: String, title: String, magnetUrl: String) {
        scope.launch {
            if (!inFlightIds.add(imdbId)) return@launch  // already in-flight
            if (downloads.value.any { it.imdbId == imdbId }) {
                inFlightIds.remove(imdbId)
                return@launch
            }
            try {
                // Set activeDownloadImdbId BEFORE any suspend call so that cancelDownload's
                // compareAndSet can always find it from the moment this try body begins.
                // The finally block always clears it regardless of how we exit.
                activeDownloadImdbId.set(imdbId)
                val saveDir = context.getExternalFilesDir("downloads") ?: context.filesDir
                val entity = DownloadEntity(
                    imdbId = imdbId,
                    title = title,
                    magnetUrl = magnetUrl,
                    filePath = saveDir.absolutePath,
                )
                downloadDao.insert(entity)
                // If cancelDownload fired concurrently it cleared activeDownloadImdbId via CAS
                // and removed from inFlightIds. Re-check: if we're no longer active, abort.
                if (activeDownloadImdbId.get() != imdbId) {
                    downloadDao.delete(imdbId)
                    return@launch
                }
                val torrent = Torrent(
                    url = magnetUrl,
                    magnet = magnetUrl,
                    quality = "",
                    type = "download",
                    size = 0L,
                    fileSize = "",
                    seeds = 0,
                    peers = 0,
                    hash = "",
                )
                torrentEngine.startStream(torrent, saveDir)
                // Wait for Ready or Error state
                val finalState = torrentEngine.state.first { it is StreamState.Ready || it is StreamState.Error }
                if (finalState is StreamState.Error) {
                    downloadDao.delete(imdbId)
                    activeDownloadImdbId.compareAndSet(imdbId, null)
                    return@launch
                }
                // Ready path — use markComplete (UPDATE) so a pre-existing row is updated
                // correctly regardless of the DAO's OnConflictStrategy.
                if (finalState is StreamState.Ready) {
                    val videoFilePath = torrentEngine.getVideoFilePath() ?: saveDir.absolutePath
                    downloadDao.markComplete(imdbId, videoFilePath, System.currentTimeMillis())
                    activeDownloadImdbId.compareAndSet(imdbId, null)
                }
            } finally {
                // Always clean up, even if an uncaught exception escapes the try body
                // (e.g. torrentEngine.startStream throws). Without this, activeDownloadImdbId
                // would stay set permanently, causing cancelDownload for any subsequent
                // download to fail its CAS and never call stopCurrent().
                activeDownloadImdbId.compareAndSet(imdbId, null)
                inFlightIds.remove(imdbId)
            }
        }
    }

    fun cancelDownload(imdbId: String) {
        // Remove from inFlightIds first — acts as a cancellation signal so that a concurrent
        // startDownload coroutine still between inFlightIds.add and activeDownloadImdbId.set
        // will detect the cancellation and abort before starting the torrent engine.
        inFlightIds.remove(imdbId)
        scope.launch {
            // compareAndSet atomically clears the active-ID only when it still matches,
            // preventing a TOCTOU race where another coroutine changes activeDownloadImdbId
            // between our read and the stopCurrent() call.
            if (activeDownloadImdbId.compareAndSet(imdbId, null)) {
                torrentEngine.stopCurrent()
            }
            downloadDao.delete(imdbId)
        }
    }

    fun deleteDownload(imdbId: String) {
        scope.launch {
            val entity = downloads.value.find { it.imdbId == imdbId }
            entity?.filePath?.let { path ->
                val f = java.io.File(path)
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            }
            downloadDao.delete(imdbId)
        }
    }
}
