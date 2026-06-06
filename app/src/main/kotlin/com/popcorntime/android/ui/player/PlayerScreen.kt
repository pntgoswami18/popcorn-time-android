package com.popcorntime.android.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.popcorntime.android.data.subtitles.Subtitle
import com.popcorntime.android.domain.model.StreamState

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    imdbId: String,
    quality: String,
    season: Int = -1,
    episode: Int = -1,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val streamState by viewModel.streamState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            viewModel.stopStream()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        when (val s = streamState) {
            is StreamState.Idle -> LoadingIndicator()
            is StreamState.Buffering -> BufferingOverlay(s.progress, s.downloadSpeed, s.seeds, s.peers)
            is StreamState.Ready -> {
                ExoPlayerView(
                    streamUrl = s.streamUrl,
                    subtitleUrl = uiState.subtitleUrl,
                    viewModel = viewModel,
                )
                StreamStatsOverlay(
                    dlSpeed = s.downloadSpeed,
                    ulSpeed = s.uploadSpeed,
                    seeds = s.seeds,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 8.dp),
                )
            }
            is StreamState.Error -> ErrorView(s.message, onBack)
        }

        // Top controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            IconButton(onClick = viewModel::toggleSubtitlePicker) {
                Icon(
                    imageVector = if (uiState.selectedSubtitle != null) Icons.Default.ClosedCaption
                                  else Icons.Default.ClosedCaptionOff,
                    contentDescription = "Subtitles",
                    tint = if (uiState.selectedSubtitle != null) MaterialTheme.colorScheme.primary
                           else Color.White,
                )
            }
        }

        // Subtitle picker bottom sheet
        if (uiState.showSubtitlePicker) {
            SubtitlePicker(
                subtitles = uiState.subtitles,
                selectedSubtitle = uiState.selectedSubtitle,
                isLoading = uiState.isLoadingSubtitles,
                onSelect = viewModel::selectSubtitle,
                onDismiss = viewModel::toggleSubtitlePicker,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerView(streamUrl: String, subtitleUrl: String?, viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(streamUrl, subtitleUrl) {
        val mediaItem = if (subtitleUrl != null) {
            MediaItem.Builder()
                .setUri(streamUrl)
                .setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUrl))
                            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                            .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                            .build()
                    )
                ).build()
        } else {
            MediaItem.fromUri(streamUrl)
        }
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        onDispose { exoPlayer.release() }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.onPlaybackCompleted()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setShowSubtitleButton(true)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SubtitlePicker(
    subtitles: List<Subtitle>,
    selectedSubtitle: Subtitle?,
    isLoading: Boolean,
    onSelect: (Subtitle?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(max = 320.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Subtitles", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                LazyColumn {
                    item {
                        SubtitleRow(
                            label = "None",
                            isSelected = selectedSubtitle == null,
                            onClick = { onSelect(null) },
                        )
                    }
                    items(subtitles) { sub ->
                        SubtitleRow(
                            label = sub.label,
                            isSelected = selectedSubtitle?.fileId == sub.fileId,
                            onClick = { onSelect(sub) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BufferingOverlay(progress: Float, dlSpeed: Long, seeds: Int, peers: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text("Buffering ${(progress * 100).toInt()}%", color = Color.White,
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("${dlSpeed.toHumanSpeed()} ↓   $seeds seeds   $peers peers",
            color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StreamStatsOverlay(dlSpeed: Long, ulSpeed: Long, seeds: Int, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.Black.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.small) {
        Text(
            "↓${dlSpeed.toHumanSpeed()}  ↑${ulSpeed.toHumanSpeed()}  $seeds seeds",
            color = Color.White, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ErrorView(message: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Go back") }
    }
}

private fun Long.toHumanSpeed(): String = when {
    this < 1024 -> "${this}B/s"
    this < 1024 * 1024 -> "${"%.1f".format(this / 1024.0)}KB/s"
    else -> "${"%.1f".format(this / (1024.0 * 1024))}MB/s"
}
