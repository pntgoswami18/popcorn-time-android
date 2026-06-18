package com.popcorntime.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.db.entity.DownloadEntity
import com.popcorntime.android.data.torrent.DownloadManager
import com.popcorntime.android.data.torrent.DownloadStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "wmv", "flv")

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
) : ViewModel() {
    val downloads: StateFlow<List<DownloadEntity>> = downloadManager.downloads
    val activeDownloadStats: StateFlow<DownloadStats?> = downloadManager.activeDownloadStats
    val activeImdbId: StateFlow<String?> = downloadManager.activeImdbId

    /** Playable local URIs for completed downloads, resolved off the main thread.
     *  Recomputed whenever the downloads list changes. */
    val playableUris: StateFlow<Map<String, String?>> = downloadManager.downloads
        .map { list ->
            list.filter { it.completedAt != null }
                .associate { it.imdbId to resolvePlayableVideoUri(it.filePath) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun cancelDownload(imdbId: String) {
        downloadManager.cancelDownload(imdbId)
    }

    fun deleteDownload(imdbId: String) {
        viewModelScope.launch { downloadManager.deleteDownload(imdbId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayDownload: (localUri: String) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        DownloadsTabContent(
            onPlayDownload = onPlayDownload,
            modifier = Modifier.padding(padding),
            viewModel = viewModel,
        )
    }
}

@Composable
fun DownloadsTabContent(
    onPlayDownload: (localUri: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val activeStats by viewModel.activeDownloadStats.collectAsStateWithLifecycle()
    val activeImdbId by viewModel.activeImdbId.collectAsStateWithLifecycle()
    val playableUris by viewModel.playableUris.collectAsStateWithLifecycle()

    if (downloads.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No downloads yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(downloads, key = { it.imdbId }) { download ->
                val playableUri = playableUris[download.imdbId]
                DownloadItem(
                    download = download,
                    activeStats = activeStats?.takeIf { it.imdbId == download.imdbId },
                    isActive = download.imdbId == (activeImdbId ?: activeStats?.imdbId),
                    playableUri = playableUri,
                    onPlay = { playableUri?.let(onPlayDownload) },
                    onCancel = { viewModel.cancelDownload(download.imdbId) },
                    onDelete = { viewModel.deleteDownload(download.imdbId) },
                )
            }
        }
    }
}

@Composable
private fun DownloadItem(
    download: DownloadEntity,
    activeStats: DownloadStats?,
    isActive: Boolean,
    playableUri: String?,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val isCompleted = download.completedAt != null
    val isPlayable = isCompleted && playableUri != null
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (download.quality.isNotBlank()) {
                        Text(
                            text = download.quality,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isCompleted && isActive) {
                        IconButton(onClick = onCancel) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (isCompleted) {
                        IconButton(
                            onClick = onPlay,
                            enabled = isPlayable,
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isPlayable) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (isCompleted) {
                Text(
                    "Completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (activeStats != null) {
                LinearProgressIndicator(
                    progress = { activeStats.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    formatDownloadStatsLabel(activeStats),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (isActive) {
                // Actively downloading but no stats yet — show indeterminate progress.
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Starting…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Queued",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Builds the "45% · 12.3 MB / 700.0 MB · 512 KB/s" stats line for an in-progress download.
 *  Pure and JVM-testable. Never embeds runtime values into a format string, so values
 *  containing '%' or other format-significant characters cannot crash the formatter. */
internal fun formatDownloadStatsLabel(stats: DownloadStats): String {
    val pct = (stats.progress * 100).toInt()
    val dlMb = stats.downloadedBytes / 1_048_576f
    val totalMb = stats.totalBytes / 1_048_576f
    val speedKb = stats.downloadSpeedBps / 1024f
    val sizeText = if (stats.totalBytes > 0) {
        "%.1f MB / %.1f MB".format(Locale.US, dlMb, totalMb)
    } else {
        "%.1f MB downloaded".format(Locale.US, dlMb)
    }
    val speedText = "%.0f".format(Locale.US, speedKb)
    return "$pct% · $sizeText · $speedText KB/s"
}

internal fun resolvePlayableVideoUri(filePath: String?): String? {
    if (filePath.isNullOrBlank()) return null
    if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
        return filePath
    }
    val file = File(filePath)
    if (file.isFile && file.length() > 0L) {
        return file.toURI().toString()
    }
    if (file.isDirectory) {
        val videoFile = file.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in VIDEO_EXTENSIONS && it.length() > 0L }
            .maxByOrNull { it.length() }
        return videoFile?.toURI()?.toString()
    }
    return null
}
