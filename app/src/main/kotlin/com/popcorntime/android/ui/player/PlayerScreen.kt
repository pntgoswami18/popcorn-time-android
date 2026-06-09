package com.popcorntime.android.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.popcorntime.android.MainActivity
import com.popcorntime.android.data.cast.DlnaRenderer
import com.popcorntime.android.data.remote.PlaybackCommand
import com.popcorntime.android.data.remote.PlaybackController
import com.popcorntime.android.data.subtitles.Subtitle
import com.popcorntime.android.data.subtitles.SubtitleStyle
import com.popcorntime.android.data.subtitles.SubtitleStyleStore
import com.popcorntime.android.domain.model.CastState
import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.ui.cast.CastBottomSheet
import com.popcorntime.android.ui.cast.CastOverlay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    imdbId: String,
    quality: String,
    season: Int = -1,
    episode: Int = -1,
    contentType: String = "movie",
    localUri: String? = null,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val streamState by viewModel.streamState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val countdownSeconds by viewModel.countdownSeconds.collectAsStateWithLifecycle()
    val resizeMode by viewModel.resizeMode.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()

    // Set screen orientation to landscape
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            viewModel.stopStream()
        }
    }

    // Signal PiP visibility to MainActivity + detect PiP mode
    val mainActivity = context as? MainActivity
    DisposableEffect(Unit) {
        mainActivity?.setPlayerVisible(true)
        onDispose { mainActivity?.setPlayerVisible(false) }
    }

    val isInPip by (mainActivity?.isInPip ?: remember { mutableStateOf(false) })

    // Screen brightness
    val window = (context as Activity).window
    LaunchedEffect(brightness) {
        window.attributes = window.attributes.also {
            it.screenBrightness = if (brightness < 0f)
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            else brightness
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            window.attributes = window.attributes.also {
                it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Determine stream URL: local file or torrent stream
        val effectiveStreamUrl = localUri ?: uiState.streamUrl
            ?: (streamState as? StreamState.Ready)?.streamUrl

        if (localUri != null) {
            // Local file player
            if (effectiveStreamUrl != null) {
                ExoPlayerView(
                    streamUrl = effectiveStreamUrl,
                    subtitleUrl = uiState.subtitleUrl,
                    viewModel = viewModel,
                    playbackController = viewModel.playbackController,
                    resizeMode = resizeMode,
                )
            }
        } else {
            when (val s = streamState) {
                is StreamState.Idle -> LoadingIndicator()
                is StreamState.Buffering -> BufferingOverlay(s.progress, s.downloadSpeed, s.seeds, s.peers)
                is StreamState.Ready -> {
                    ExoPlayerView(
                        streamUrl = s.streamUrl,
                        subtitleUrl = uiState.subtitleUrl,
                        viewModel = viewModel,
                        playbackController = viewModel.playbackController,
                        resizeMode = resizeMode,
                    )
                    if (!isInPip) {
                        StreamStatsOverlay(
                            dlSpeed = s.downloadSpeed,
                            ulSpeed = s.uploadSpeed,
                            seeds = s.seeds,
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 8.dp),
                        )
                    }
                }
                is StreamState.Error -> ErrorView(s.message, onBack)
            }
        }

        if (!isInPip) {
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
                Row {
                    // Aspect ratio cycle
                    IconButton(onClick = viewModel::cycleResizeMode) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }
                    // Brightness
                    var showBrightnessDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showBrightnessDialog = true }) {
                        Icon(Icons.Default.BrightnessHigh, contentDescription = "Brightness", tint = Color.White)
                    }
                    if (showBrightnessDialog) {
                        AlertDialog(
                            onDismissRequest = { showBrightnessDialog = false },
                            title = { Text("Brightness") },
                            text = {
                                Slider(
                                    value = brightness.coerceAtLeast(0f),
                                    onValueChange = viewModel::setBrightness,
                                    valueRange = 0f..1f,
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showBrightnessDialog = false }) { Text("OK") }
                            },
                        )
                    }
                    // Audio track picker
                    var showAudioPicker by remember { mutableStateOf(false) }
                    IconButton(onClick = { showAudioPicker = true }) {
                        Icon(Icons.Default.Headset, contentDescription = "Audio Track", tint = Color.White)
                    }
                    // Cast
                    val isCasting = uiState.castState is CastState.Connected
                    IconButton(onClick = viewModel::toggleCastSheet) {
                        Icon(
                            imageVector = if (isCasting) Icons.Default.CastConnected else Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = if (isCasting) MaterialTheme.colorScheme.primary else Color.White,
                        )
                    }
                    // Subtitles
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
            }

            // Subtitle picker
            if (uiState.showSubtitlePicker) {
                SubtitlePicker(
                    subtitles = uiState.subtitles,
                    selectedSubtitle = uiState.selectedSubtitle,
                    isLoading = uiState.isLoadingSubtitles,
                    onSelect = viewModel::selectSubtitle,
                    onImportFromDevice = viewModel::loadCustomSubtitle,
                    onDismiss = viewModel::toggleSubtitlePicker,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Cast bottom sheet
            if (uiState.showCastSheet) {
                CastBottomSheet(
                    castState = uiState.castState,
                    kodiAddress = uiState.kodiAddress,
                    dlnaRenderers = uiState.dlnaRenderers,
                    onChromecastClick = viewModel::castToChromecast,
                    onExternalPlayerClick = viewModel::castToExternalPlayer,
                    onKodiConnect = viewModel::castToKodi,
                    onDlnaSelect = viewModel::castToDlna,
                    onDismiss = viewModel::toggleCastSheet,
                )
            }

            // Cast overlay
            val castState = uiState.castState
            if (castState is CastState.Connected) {
                CastOverlay(
                    castState = castState,
                    onDisconnect = viewModel::disconnectCast,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Auto-play countdown overlay
            if (countdownSeconds != null) {
                CountdownOverlay(
                    seconds = countdownSeconds!!,
                    onCancel = viewModel::cancelCountdown,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerView(
    streamUrl: String,
    subtitleUrl: String?,
    viewModel: PlayerViewModel,
    playbackController: PlaybackController,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    subtitleStyleStore: SubtitleStyleStore? = null,
) {
    val context = LocalContext.current
    var playbackStarted by remember { mutableStateOf(false) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // Media Session
    DisposableEffect(exoPlayer) {
        val session = MediaSession.Builder(context, exoPlayer).build()
        onDispose { session.release() }
    }

    // Collect playback commands
    LaunchedEffect(playbackController) {
        playbackController.command.collect { cmd ->
            when (cmd) {
                is PlaybackCommand.Play -> exoPlayer.play()
                is PlaybackCommand.Pause -> exoPlayer.pause()
                is PlaybackCommand.SeekTo -> exoPlayer.seekTo(cmd.positionMs)
                is PlaybackCommand.Stop -> exoPlayer.stop()
            }
        }
    }

    // Report position/duration/isPlaying
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playbackController.updatePosition(
                    player.currentPosition,
                    player.duration.coerceAtLeast(0),
                    player.isPlaying,
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Rebuild media item when stream or subtitle changes
    LaunchedEffect(streamUrl, subtitleUrl) {
        val isLocalUri = streamUrl.startsWith("content://") || streamUrl.startsWith("file://")
        val mediaItemBuilder = MediaItem.Builder().setUri(streamUrl)
        if (subtitleUrl != null && !isLocalUri) {
            val subtitleMime = if (subtitleUrl.endsWith(".vtt", ignoreCase = true))
                androidx.media3.common.MimeTypes.TEXT_VTT
            else
                androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
            val subtitle = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUrl))
                .setMimeType(subtitleMime)
                .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
        }
        exoPlayer.setMediaItem(mediaItemBuilder.build())
        playbackStarted = false
        exoPlayer.prepare()
    }

    // Playback completion listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> playbackStarted = true
                    Player.STATE_ENDED -> if (playbackStarted) {
                        playbackStarted = false
                        viewModel.onPlaybackCompleted()
                    }
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
                this.resizeMode = resizeMode
            }
        },
        update = { playerView ->
            playerView.resizeMode = resizeMode
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CountdownOverlay(
    seconds: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(
                progress = { seconds / 10f },
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
            Text(
                text = "Next episode in ${seconds}s",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onCancel) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SubtitlePicker(
    subtitles: List<Subtitle>,
    selectedSubtitle: Subtitle?,
    isLoading: Boolean,
    onSelect: (Subtitle?) -> Unit,
    onImportFromDevice: (android.net.Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportFromDevice(it) }
    }

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
                    item {
                        // Import from device
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { launcher.launch(arrayOf("*/*")) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.FileCopy, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import from device",
                                style = MaterialTheme.typography.bodyMedium)
                        }
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
