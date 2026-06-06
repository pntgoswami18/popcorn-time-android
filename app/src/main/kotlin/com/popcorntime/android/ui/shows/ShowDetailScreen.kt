package com.popcorntime.android.ui.shows

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                                if (state.isInWatchlist) Icons.Default.PlaylistAddCheck else Icons.Default.PlaylistAdd,
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
                modifier = Modifier.padding(padding),
                onSeasonSelect = viewModel::selectSeason,
                onEpisodePlay = onEpisodePlay,
            )
        }
    }
}

@Composable
private fun ShowDetailContent(
    show: Show,
    seasons: List<Season>,
    selectedSeason: Int,
    modifier: Modifier = Modifier,
    onSeasonSelect: (Int) -> Unit,
    onEpisodePlay: (String, Int, Int, String) -> Unit,
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
                Spacer(Modifier.height(16.dp))
            }
        }

        // Season tabs
        item {
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

        // Episodes for selected season
        val currentSeason = seasons.find { it.number == selectedSeason }
        items(currentSeason?.episodes ?: emptyList(), key = { it.tvdbId }) { episode ->
            EpisodeRow(
                episode = episode,
                onClick = { quality ->
                    onEpisodePlay(show.imdbId, episode.season, episode.episode, quality)
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onClick: (quality: String) -> Unit) {
    var showQualityPicker by remember { mutableStateOf(false) }
    val availableQualities = episode.torrents.keys
        .filter { it != "0" }
        .sortedByDescending { qualityRank(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = availableQualities.isNotEmpty()) { showQualityPicker = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "E${episode.episode} · ${episode.title}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (episode.overview.isNotBlank()) {
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        if (availableQualities.isNotEmpty()) {
            Icon(Icons.Default.PlayCircle, contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        } else {
            Icon(Icons.Default.CloudOff, contentDescription = "No torrents",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp))
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

private fun qualityRank(q: String) = when (q) { "1080p" -> 3; "720p" -> 2; "480p" -> 1; else -> 0 }
