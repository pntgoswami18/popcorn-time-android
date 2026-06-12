package com.popcorntime.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "wmv", "flv")

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
) : ViewModel() {
    val downloads: StateFlow<List<DownloadEntity>> = downloadManager.downloads
    val activeDownloadStats: StateFlow<DownloadStats?> = downloadManager.activeDownloadStats

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
                DownloadItem(
                    download = download,
                    activeStats = activeStats?.takeIf { it.imdbId == download.imdbId },
                    onPlay = {
                        resolvePlayableVideoUri(download.filePath)?.let(onPlayDownload)
                    },
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
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val isCompleted = download.completedAt != null
    val isPlayable = isCompleted && resolvePlayableVideoUri(download.filePath) != null
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
                val pct = (activeStats.progress * 100).toInt()
                val dlMb = activeStats.downloadedBytes / 1_048_576f
                val totalMb = activeStats.totalBytes / 1_048_576f
                val speedKb = activeStats.downloadSpeedBps / 1024f
                val sizeText = if (activeStats.totalBytes > 0)
                    "%.1f MB / %.1f MB".format(dlMb, totalMb)
                else
                    "%.1f MB downloaded".format(dlMb)
                Text(
                    "$pct% · $sizeText · %.0f KB/s".format(speedKb),
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
