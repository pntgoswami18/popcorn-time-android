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

    fun startDownload(imdbId: String, title: String, magnetUrl: String) {
        scope.launch {
            if (downloads.value.any { it.imdbId == imdbId }) return@launch  // already tracked
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
            // Wait for Ready state to capture actual file path
            val readyState = torrentEngine.state.first { it is StreamState.Ready }
            if (readyState is StreamState.Ready) {
                val videoFilePath = torrentEngine.getVideoFilePath() ?: saveDir.absolutePath
                val updatedEntity = entity.copy(
                    filePath = videoFilePath,
                    completedAt = System.currentTimeMillis(),
                )
                downloadDao.insert(updatedEntity)
            }
        }
    }

    fun cancelDownload(imdbId: String) {
        scope.launch {
            if (activeDownloadImdbId.get() == imdbId) {
                torrentEngine.stopCurrent()
                activeDownloadImdbId.set(null)
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
