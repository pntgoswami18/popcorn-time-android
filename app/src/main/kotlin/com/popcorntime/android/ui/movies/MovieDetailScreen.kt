package com.popcorntime.android.ui.movies

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.popcorntime.android.domain.model.Movie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    imdbId: String,
    onBack: () -> Unit,
    onPlayClick: (quality: String) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(imdbId) { viewModel.loadMovie(imdbId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.movie?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.movie?.let { movie ->
                        IconButton(onClick = { viewModel.toggleBookmark() }) {
                            Icon(
                                if (state.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = { viewModel.toggleWatchlist() }) {
                            Icon(
                                if (state.isInWatchlist) Icons.Default.PlaylistAddCheck else Icons.Default.PlaylistAdd,
                                contentDescription = "Watchlist",
                                tint = if (state.isInWatchlist) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = { viewModel.toggleWatched() }) {
                            Icon(
                                if (state.isWatched) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Watched",
                                tint = if (state.isWatched) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.movie != null -> MovieDetailContent(
                movie = state.movie!!,
                isWatched = state.isWatched,
                isBookmarked = state.isBookmarked,
                selectedQuality = state.selectedQuality,
                modifier = Modifier.padding(padding),
                onQualitySelect = viewModel::selectQuality,
                onPlayClick = { onPlayClick(state.selectedQuality) },
                onTrailerClick = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
            )
        }
    }
}

@Composable
private fun MovieDetailContent(
    movie: Movie,
    isWatched: Boolean,
    isBookmarked: Boolean,
    selectedQuality: String,
    modifier: Modifier = Modifier,
    onQualitySelect: (String) -> Unit,
    onPlayClick: () -> Unit,
    onTrailerClick: (String) -> Unit,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // Backdrop + poster hero
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            AsyncImage(
                model = movie.backdropUrl.ifBlank { movie.posterUrl },
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

        // Metadata row
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(movie.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingBadge(movie.rating)
                Text(movie.year.toString(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (movie.runtime > 0) {
                    Text("${movie.runtime}m", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (movie.certification.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            movie.certification,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Genres
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                movie.genres.forEach { genre ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ) {
                        Text(
                            genre,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Synopsis
            Text(movie.synopsis, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))

            // Quality selector
            if (movie.torrents.isNotEmpty()) {
                Text("Quality", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    movie.torrents.keys.sortedDescending().forEach { quality ->
                        val torrent = movie.torrents[quality]!!
                        FilterChip(
                            selected = selectedQuality == quality,
                            onClick = { onQualitySelect(quality) },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(quality)
                                    Text(
                                        "↑${torrent.seeds}  ↓${torrent.peers}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlayClick,
                    enabled = movie.torrents.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Now")
                }
                if (movie.trailerUrl != null) {
                    OutlinedButton(
                        onClick = { onTrailerClick(movie.trailerUrl) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Trailer")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RatingBadge(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "%.1f".format(rating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
