package com.popcorntime.android.ui.shows

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.popcorntime.android.R
import com.popcorntime.android.domain.model.Episode
import com.popcorntime.android.domain.model.Season
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.ui.settings.formatDownloadStatsLabel
import java.text.SimpleDateFormat
import java.util.*

// ── Screen entry point ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShowDetailScreen(
    imdbId: String,
    onBack: () -> Unit,
    onEpisodePlay: (imdbId: String, season: Int, episode: Int, quality: String) -> Unit,
    onPlayDownloaded: (localUri: String) -> Unit = {},
    viewModel: ShowDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.transientMessageVersion) {
        state.transientMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeTransientMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.show?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    state.show?.let {
                        IconButton(onClick = viewModel::toggleWatched) {
                            Icon(
                                imageVector = if (state.isWatched) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (state.isWatched) "Mark unwatched" else "Mark watched",
                                tint = if (state.isWatched) MaterialTheme.colorScheme.primary
                                       else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = viewModel::toggleBookmark) {
                            Icon(
                                if (state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Favourite",
                                tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary
                                       else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = viewModel::toggleWatchlist) {
                            Icon(
                                if (state.isInWatchlist) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Filled.PlaylistAdd,
                                "Watchlist",
                                tint = if (state.isInWatchlist) MaterialTheme.colorScheme.primary
                                       else LocalContentColor.current,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.show == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.error != null && state.show == null -> Box(
                Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.show != null -> ShowDetailContent(
                show = state.show!!,
                seasons = state.seasons,
                selectedSeason = state.selectedSeason,
                allWatched = state.allWatched,
                isEpisodesLoading = state.isEpisodesLoading,
                episodesError = state.episodesError,
                watchedEpisodeKeys = state.watchedEpisodeKeys,
                episodeDownloads = state.episodeDownloads,
                modifier = Modifier.padding(padding),
                onSeasonSelect = viewModel::selectSeason,
                onEpisodePlay = onEpisodePlay,
                onMarkAllWatched = viewModel::markAllWatched,
                onToggleEpisodeWatched = viewModel::toggleEpisodeWatched,
                onRetryEpisodes = viewModel::retryEpisodes,
                onStartDownload = viewModel::startEpisodeDownload,
                onCancelDownload = viewModel::cancelEpisodeDownload,
                onPlayDownloaded = onPlayDownloaded,
            )
        }
    }
}

// ── Detail content ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowDetailContent(
    show: Show,
    seasons: List<Season>,
    selectedSeason: Int,
    allWatched: Boolean = false,
    isEpisodesLoading: Boolean = false,
    episodesError: String? = null,
    watchedEpisodeKeys: Set<String> = emptySet(),
    episodeDownloads: Map<String, EpisodeDownloadState> = emptyMap(),
    modifier: Modifier = Modifier,
    onSeasonSelect: (Int) -> Unit,
    onEpisodePlay: (String, Int, Int, String) -> Unit,
    onMarkAllWatched: () -> Unit = {},
    onToggleEpisodeWatched: (Episode) -> Unit = {},
    onRetryEpisodes: () -> Unit = {},
    onStartDownload: (Episode, String) -> Unit = { _, _ -> },
    onCancelDownload: (Episode) -> Unit = {},
    onPlayDownloaded: (localUri: String) -> Unit = {},
) {
    // Track which episode's action sheet is open (null = closed)
    var sheetEpisode by remember { mutableStateOf<Episode?>(null) }

    // Action sheet — renders as an overlay outside the LazyColumn
    sheetEpisode?.let { episode ->
        val epKey = "${show.imdbId}_s${episode.season}e${episode.episode}"
        val dlState = episodeDownloads[epKey] ?: EpisodeDownloadState.NotDownloaded
        EpisodeActionSheet(
            show = show,
            episode = episode,
            downloadState = dlState,
            onStream = { quality ->
                onEpisodePlay(show.imdbId, episode.season, episode.episode, quality)
                sheetEpisode = null
            },
            onDownload = { quality ->
                onStartDownload(episode, quality)
                sheetEpisode = null
            },
            onCancelDownload = {
                onCancelDownload(episode)
                sheetEpisode = null
            },
            onPlayDownloaded = { uri ->
                onPlayDownloaded(uri)
                sheetEpisode = null
            },
            onDismiss = { sheetEpisode = null },
        )
    }

    LazyColumn(modifier = modifier) {

        // ── Hero backdrop ─────────────────────────────────────────────────────
        item {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = show.backdropUrl.ifBlank { show.posterUrl },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.posterholder),
                    error = painterResource(R.drawable.posterholder),
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                            startY = 80f,
                        )
                    )
                )
            }
        }

        // ── Meta ──────────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(show.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("%.1f".format(show.rating), color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(show.year, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (show.numSeasons > 0) Text("${show.numSeasons} Seasons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (show.status.isNotBlank()) Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (show.status == "Continuing")
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(show.status, style = MaterialTheme.typography.labelMedium,
                            color = if (show.status == "Continuing")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (show.network.isNotBlank() || show.airDay.isNotBlank()) {
                    Text(
                        buildString {
                            if (show.network.isNotBlank()) append(show.network)
                            if (show.airDay.isNotBlank()) append(" · ${show.airDay}s ${show.airTime}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    show.genres.forEach { genre ->
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                            Text(genre, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(show.synopsis, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onMarkAllWatched,
                    enabled = !allWatched && !isEpisodesLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (allWatched) "All episodes marked watched" else "Mark all watched")
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Season tabs / loading / error ─────────────────────────────────────
        item {
            when {
                isEpisodesLoading -> {
                    val shimmer = rememberShimmerBrush()
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(80.dp).height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(shimmer),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                episodesError != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text("Could not load episodes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = onRetryEpisodes) { Text("Retry") }
                    }
                }
                seasons.isEmpty() -> {
                    Text(
                        "No episode data available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        seasons.forEach { season ->
                            FilterChip(
                                selected = selectedSeason == season.number,
                                onClick = { onSeasonSelect(season.number) },
                                label = { Text("Season ${season.number}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Skeleton rows while loading ───────────────────────────────────────
        if (isEpisodesLoading) {
            items(5) {
                EpisodeRowSkeleton()
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        // ── Episode rows ──────────────────────────────────────────────────────
        if (!isEpisodesLoading && episodesError == null) {
            val currentSeason = seasons.find { it.number == selectedSeason }
            items(currentSeason?.episodes ?: emptyList(), key = { it.tvdbId }) { episode ->
                val epKey = "${show.imdbId}_s${episode.season}e${episode.episode}"
                EpisodeRow(
                    episode = episode,
                    isWatched = epKey in watchedEpisodeKeys,
                    downloadState = episodeDownloads[epKey] ?: EpisodeDownloadState.NotDownloaded,
                    onToggleWatched = { onToggleEpisodeWatched(episode) },
                    onRowClick = { sheetEpisode = episode },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Episode action bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeActionSheet(
    show: Show,
    episode: Episode,
    downloadState: EpisodeDownloadState,
    onStream: (quality: String) -> Unit,
    onDownload: (quality: String) -> Unit,
    onCancelDownload: () -> Unit,
    onPlayDownloaded: (localUri: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val availableQualities = episode.torrents.keys
        .filter { it != "0" }
        .sortedByDescending { qualityRank(it) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Episode title header
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "S${episode.season.toString().padStart(2, '0')}E${episode.episode.toString().padStart(2, '0')} · ${episode.title}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = show.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (availableQualities.isEmpty()) {
            // No sources
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.CloudOff, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
                Column {
                    Text("No sources available", style = MaterialTheme.typography.bodyMedium)
                    Text("No torrents found for this episode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // ── Play downloaded row (if complete) ─────────────────────────────
            if (downloadState is EpisodeDownloadState.Downloaded) {
                SheetSectionLabel("Downloaded")
                downloadState.localUri?.let { uri ->
                    SheetActionRow(
                        icon = Icons.Default.PlayCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        label = "Play downloaded file",
                        subtitle = null,
                        onClick = { onPlayDownloaded(uri) },
                    )
                } ?: run {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Text("Downloaded", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ── Queued / Downloading row ──────────────────────────────────────
            if (downloadState is EpisodeDownloadState.Queued ||
                downloadState is EpisodeDownloadState.Downloading) {
                SheetSectionLabel("Download in progress")
                when (downloadState) {
                    is EpisodeDownloadState.Queued -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Queued…", modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onCancelDownload) { Text("Cancel") }
                        }
                    }
                    is EpisodeDownloadState.Downloading -> {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    formatDownloadStatsLabel(downloadState.stats),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onCancelDownload) { Text("Cancel") }
                            }
                            LinearProgressIndicator(
                                progress = { downloadState.stats.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    else -> Unit
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ── Stream section ────────────────────────────────────────────────
            SheetSectionLabel("Stream")
            availableQualities.forEach { quality ->
                val torrent = episode.torrents[quality]
                SheetActionRow(
                    icon = Icons.Default.PlayArrow,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = quality,
                    subtitle = torrent?.let { "↑${it.seeds} seeds · ${it.provider}" },
                    onClick = { onStream(quality) },
                )
            }

            // ── Download section (only if not already downloading/downloaded) ─
            if (downloadState is EpisodeDownloadState.NotDownloaded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SheetSectionLabel("Download")
                availableQualities.forEach { quality ->
                    val torrent = episode.torrents[quality]
                    SheetActionRow(
                        icon = Icons.Default.Download,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        label = quality,
                        subtitle = torrent?.let { "↑${it.seeds} seeds · ${it.provider}" },
                        onClick = { onDownload(quality) },
                    )
                }
            }
        }

        // Bottom nav bar inset
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Episode row ───────────────────────────────────────────────────────────────

@Composable
private fun EpisodeRow(
    episode: Episode,
    isWatched: Boolean,
    downloadState: EpisodeDownloadState,
    onToggleWatched: () -> Unit,
    onRowClick: () -> Unit,
) {
    val hasSource = episode.torrents.any { it.key != "0" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Episode thumbnail
        if (episode.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.posterholder),
                error = painterResource(R.drawable.posterholder),
                modifier = Modifier
                    .size(width = 100.dp, height = 60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Tv, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp))
            }
        }

        // Text info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "E${episode.episode} · ${episode.title}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isWatched)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface,
            )
            if (episode.overview.isNotBlank()) {
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isWatched) 0.4f else 1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (episode.firstAired > 0) {
                Text(
                    text = SimpleDateFormat("MMM d, yyyy", Locale.US)
                        .format(Date(episode.firstAired * 1000)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        // Right column: watched toggle + download/play state
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Watched toggle
            IconButton(onClick = onToggleWatched, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = if (isWatched) "Mark unwatched" else "Mark watched",
                    tint = if (isWatched) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Download / play state indicator
            when (downloadState) {
                is EpisodeDownloadState.NotDownloaded -> {
                    if (hasSource) {
                        Icon(Icons.Default.PlayCircle, "Stream/download",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Default.CloudOff, "No sources",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp))
                    }
                }
                is EpisodeDownloadState.Queued -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is EpisodeDownloadState.Downloading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                        CircularProgressIndicator(
                            progress = { downloadState.stats.progress },
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${(downloadState.stats.progress * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                is EpisodeDownloadState.Downloaded -> {
                    Icon(Icons.Default.DownloadDone, "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ── Skeleton shimmer ──────────────────────────────────────────────────────────

@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + 600f, 0f),
    )
}

@Composable
private fun EpisodeRowSkeleton() {
    val shimmer = rememberShimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Box(Modifier.fillMaxWidth(0.85f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
            Box(Modifier.fillMaxWidth(0.35f).height(9.dp).clip(RoundedCornerShape(4.dp)).background(shimmer))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(shimmer))
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(shimmer))
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun qualityRank(q: String) = when (q) { "2160p" -> 4; "1080p" -> 3; "720p" -> 2; "480p" -> 1; else -> 0 }
