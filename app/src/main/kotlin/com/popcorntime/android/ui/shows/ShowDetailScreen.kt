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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.popcorntime.android.domain.model.Episode
import com.popcorntime.android.domain.model.Season
import com.popcorntime.android.domain.model.Show
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShowDetailScreen(
    imdbId: String,
    onBack: () -> Unit,
    onEpisodePlay: (imdbId: String, season: Int, episode: Int, quality: String) -> Unit,
    viewModel: ShowDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
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
                modifier = Modifier.padding(padding),
                onSeasonSelect = viewModel::selectSeason,
                onEpisodePlay = onEpisodePlay,
                onMarkAllWatched = viewModel::markAllWatched,
                onToggleEpisodeWatched = viewModel::toggleEpisodeWatched,
                onRetryEpisodes = viewModel::retryEpisodes,
            )
        }
    }
}

@Composable
private fun ShowDetailContent(
    show: Show,
    seasons: List<Season>,
    selectedSeason: Int,
    allWatched: Boolean = false,
    isEpisodesLoading: Boolean = false,
    episodesError: String? = null,
    watchedEpisodeKeys: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    onSeasonSelect: (Int) -> Unit,
    onEpisodePlay: (String, Int, Int, String) -> Unit,
    onMarkAllWatched: () -> Unit = {},
    onToggleEpisodeWatched: (Episode) -> Unit = {},
    onRetryEpisodes: () -> Unit = {},
) {
    LazyColumn(modifier = modifier) {
        // Hero backdrop
        item {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = show.backdropUrl.ifBlank { show.posterUrl },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
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

        // Meta
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
                // Genres
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

        // Season tabs / loading / error
        item {
            when {
                isEpisodesLoading -> {
                    // Shimmer season-tab row
                    val shimmer = rememberShimmerBrush()
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(32.dp)
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

        // Skeleton episode rows while loading
        if (isEpisodesLoading) {
            items(5) {
                EpisodeRowSkeleton()
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        // Episodes for selected season
        if (!isEpisodesLoading && episodesError == null) {
            val currentSeason = seasons.find { it.number == selectedSeason }
            items(currentSeason?.episodes ?: emptyList(), key = { it.tvdbId }) { episode ->
                val epKey = "${show.imdbId}_s${episode.season}e${episode.episode}"
                EpisodeRow(
                    episode = episode,
                    isWatched = epKey in watchedEpisodeKeys,
                    onToggleWatched = { onToggleEpisodeWatched(episode) },
                    onClick = { quality ->
                        onEpisodePlay(show.imdbId, episode.season, episode.episode, quality)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    isWatched: Boolean,
    onToggleWatched: () -> Unit,
    onClick: (quality: String) -> Unit,
) {
    var showQualityPicker by remember { mutableStateOf(false) }
    val availableQualities = episode.torrents.keys
        .filter { it != "0" }
        .sortedByDescending { qualityRank(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = availableQualities.isNotEmpty()) { showQualityPicker = true }
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

        // Action icons column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Watched toggle
            IconButton(onClick = onToggleWatched, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                    contentDescription = if (isWatched) "Mark unwatched" else "Mark watched",
                    tint = if (isWatched) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp),
                )
            }
            // Play / no-source indicator
            if (availableQualities.isNotEmpty()) {
                Icon(Icons.Default.PlayCircle, contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            } else {
                Icon(Icons.Default.CloudOff, contentDescription = "No torrents",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showQualityPicker) {
        AlertDialog(
            onDismissRequest = { showQualityPicker = false },
            title = { Text("Choose Quality") },
            text = {
                Column {
                    availableQualities.forEach { quality ->
                        val torrent = episode.torrents[quality]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClick(quality); showQualityPicker = false }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(quality, style = MaterialTheme.typography.bodyMedium)
                            if (torrent != null) {
                                Text(
                                    "↑${torrent.seeds} ↓${torrent.peers}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityPicker = false }) { Text("Cancel") }
            },
        )
    }
}

private fun qualityRank(q: String) = when (q) { "2160p" -> 4; "1080p" -> 3; "720p" -> 2; "480p" -> 1; else -> 0 }

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
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer),
        )
        // Text placeholders
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer),
            )
        }
        // Icon column placeholder
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(shimmer),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimmer),
            )
        }
    }
}
