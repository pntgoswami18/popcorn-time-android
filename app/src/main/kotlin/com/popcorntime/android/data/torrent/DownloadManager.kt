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
                val saveDir = context.getExternalFilesDir("downloads") ?: context.filesDir
                val entity = DownloadEntity(
                    imdbId = imdbId,
                    title = title,
                    magnetUrl = magnetUrl,
                    filePath = saveDir.absolutePath,
                )
                downloadDao.insert(entity)
                activeDownloadImdbId.set(imdbId)
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
                // Always remove from inFlightIds, even if an uncaught exception escapes above,
                // so future download attempts for this title are not permanently blocked.
                inFlightIds.remove(imdbId)
            }
        }
    }

    fun cancelDownload(imdbId: String) {
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
