package com.popcorntime.android.ui.shows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.popcorntime.android.R
import com.popcorntime.android.domain.model.ALL_GENRES
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.Show

val SHOW_SORT_OPTIONS = listOf("trending", "popularity", "updated", "rating", "year")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowBrowserScreen(
    contentType: ContentType,
    onShowClick: (String) -> Unit,
    viewModel: ShowBrowserViewModel = hiltViewModel(),
) {
    LaunchedEffect(contentType) { viewModel.init(contentType) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }

    val tabTitle = if (contentType == ContentType.SHOW) "Series" else "Anime"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search $tabTitle…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(tabTitle, style = MaterialTheme.typography.titleLarge)
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
            ShowFilterBar(
                selectedGenre = state.selectedGenre,
                selectedSort = state.selectedSort,
                onGenreSelect = viewModel::onGenreSelect,
                onSortSelect = viewModel::onSortSelect,
            )
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.error != null -> Column(
                    Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadShows(reset = true) }) { Text("Retry") }
                }
                else -> ShowGrid(
                    shows = state.shows,
                    watchedIds = state.watchedIds,
                    bookmarkedIds = state.bookmarkedIds,
                    isLoadingMore = state.isLoadingMore,
                    onShowClick = onShowClick,
                    onBookmarkClick = viewModel::toggleBookmark,
                    onLoadMore = { viewModel.loadShows() },
                )
            }
        }
    }
}

@Composable
private fun ShowFilterBar(
    selectedGenre: String,
    selectedSort: String,
    onGenreSelect: (String) -> Unit,
    onSortSelect: (String) -> Unit,
) {
    var genreExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            FilterChip(
                selected = selectedGenre != "All Genre",
                onClick = { genreExpanded = true },
                label = { Text(selectedGenre, maxLines = 1) },
            )
            DropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }) {
                ALL_GENRES.forEach { genre ->
                    DropdownMenuItem(
                        text = { Text(genre) },
                        onClick = { onGenreSelect(genre); genreExpanded = false },
                    )
                }
            }
        }
        Box {
            FilterChip(
                selected = selectedSort != "trending",
                onClick = { sortExpanded = true },
                label = { Text(selectedSort.replaceFirstChar { it.uppercase() }, maxLines = 1) },
            )
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                SHOW_SORT_OPTIONS.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.replaceFirstChar { it.uppercase() }) },
                        onClick = { onSortSelect(sort); sortExpanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowGrid(
    shows: List<Show>,
    watchedIds: Set<String>,
    bookmarkedIds: Set<String>,
    isLoadingMore: Boolean,
    onShowClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            when {
                shows.isEmpty() || isLoadingMore -> false
                else -> {
                    val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    last >= shows.size - 6
                }
            }
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 130.dp),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(shows, key = { it.imdbId }) { show ->
            ShowCard(
                show = show,
                isWatched = show.imdbId in watchedIds,
                isBookmarked = show.imdbId in bookmarkedIds,
                onClick = {
                    ShowCache.put(show)
                    onShowClick(show.imdbId)
                },
                onBookmarkClick = { onBookmarkClick(show.imdbId) },
            )
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ShowCard(
    show: Show,
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
            model = show.posterUrl,
            contentDescription = show.title,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.posterholder),
            error = painterResource(R.drawable.posterholder),
            modifier = Modifier.fillMaxSize(),
        )
        if (isWatched) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth().align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .padding(6.dp),
        ) {
            Column {
                Text(show.title, style = MaterialTheme.typography.labelMedium, color = Color.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("%.1f".format(show.rating), style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.width(4.dp))
                    Text(show.year, style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f))
                    if (show.numSeasons > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text("${show.numSeasons}S", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        // Rating badge
        if (show.rating > 0) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            ) {
                Text(
                    text = "★ ${"%.1f".format(show.rating)}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }

        IconButton(onClick = onBookmarkClick,
            modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(
                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

object ShowCache {
    private val map = java.util.concurrent.ConcurrentHashMap<String, Show>()
    fun put(show: Show) { map[show.imdbId] = show }
    fun get(imdbId: String): Show? = map[imdbId]
}
