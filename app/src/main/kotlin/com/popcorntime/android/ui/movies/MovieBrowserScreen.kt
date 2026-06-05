package com.popcorntime.android.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import com.popcorntime.android.domain.model.ALL_GENRES
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieBrowserScreen(
    onMovieClick: (String) -> Unit,
    viewModel: MovieBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search movies…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    } else {
                        Text("Movies", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter bar
            FilterBar(
                selectedGenre = state.selectedGenre,
                selectedSort = state.selectedSort,
                selectedQuality = state.selectedQuality,
                onGenreSelect = viewModel::onGenreSelect,
                onSortSelect = viewModel::onSortSelect,
                onQualitySelect = viewModel::onQualitySelect,
            )

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.error != null -> ErrorView(state.error!!) { viewModel.loadMovies(reset = true) }
                else -> MovieGrid(
                    movies = state.movies,
                    watchedIds = state.watchedIds,
                    bookmarkedIds = state.bookmarkedIds,
                    isLoadingMore = state.isLoadingMore,
                    onMovieClick = onMovieClick,
                    onBookmarkClick = viewModel::toggleBookmark,
                    onLoadMore = { viewModel.loadMovies() },
                )
            }
        }
    }
}

@Composable
private fun FilterBar(
    selectedGenre: String,
    selectedSort: SortOption,
    selectedQuality: String,
    onGenreSelect: (String) -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onQualitySelect: (String) -> Unit,
) {
    var genreExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var qualityExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Genre
        FilterChipDropdown(
            label = selectedGenre,
            expanded = genreExpanded,
            onToggle = { genreExpanded = !genreExpanded },
            onDismiss = { genreExpanded = false },
        ) {
            ALL_GENRES.forEach { genre ->
                DropdownMenuItem(
                    text = { Text(genre) },
                    onClick = { onGenreSelect(genre); genreExpanded = false },
                )
            }
        }

        // Sort
        FilterChipDropdown(
            label = selectedSort.label,
            expanded = sortExpanded,
            onToggle = { sortExpanded = !sortExpanded },
            onDismiss = { sortExpanded = false },
        ) {
            SortOption.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label) },
                    onClick = { onSortSelect(sort); sortExpanded = false },
                )
            }
        }

        // Quality
        FilterChipDropdown(
            label = selectedQuality,
            expanded = qualityExpanded,
            onToggle = { qualityExpanded = !qualityExpanded },
            onDismiss = { qualityExpanded = false },
        ) {
            listOf("All", "720p", "1080p", "2160p", "3D").forEach { q ->
                DropdownMenuItem(
                    text = { Text(q) },
                    onClick = { onQualitySelect(q); qualityExpanded = false },
                )
            }
        }
    }
}

@Composable
private fun FilterChipDropdown(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box {
        FilterChip(
            selected = label != "All",
            onClick = onToggle,
            label = { Text(label, maxLines = 1) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                selectedLabelColor = MaterialTheme.colorScheme.primary,
            ),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, content = content)
    }
}

@Composable
private fun MovieGrid(
    movies: List<Movie>,
    watchedIds: Set<String>,
    bookmarkedIds: Set<String>,
    isLoadingMore: Boolean,
    onMovieClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()

    // Trigger load-more when near the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= movies.size - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 130.dp),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(movies, key = { it.imdbId }) { movie ->
            MovieCard(
                movie = movie,
                isWatched = movie.imdbId in watchedIds,
                isBookmarked = movie.imdbId in bookmarkedIds,
                onClick = { onMovieClick(movie.imdbId) },
                onBookmarkClick = { onBookmarkClick(movie.imdbId) },
            )
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    isWatched: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Dim overlay for watched items
        if (isWatched) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // Bottom gradient + title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    )
                )
                .padding(6.dp),
        ) {
            Column {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "%.1f".format(movie.rating),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = movie.year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // Bookmark icon
        IconButton(
            onClick = onBookmarkClick,
            modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
