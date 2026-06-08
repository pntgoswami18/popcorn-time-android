package com.popcorntime.android.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onItemClick: (imdbId: String, contentType: LibraryContentType) -> Unit,
    onTraktSettings: () -> Unit,
    onSubtitleSettings: () -> Unit = {},
    onSourceSettings: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                actions = {
                    if (state.isTraktConnected) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            IconButton(onClick = viewModel::syncNow) {
                                Icon(Icons.Default.SyncAlt, "Sync with Trakt")
                            }
                        }
                    }
                    IconButton(onClick = onSourceSettings) {
                        Icon(Icons.Default.Storage, "Torrent Sources")
                    }
                    IconButton(onClick = onSubtitleSettings) {
                        Icon(Icons.Default.ClosedCaption, "Subtitle Settings")
                    }
                    IconButton(onClick = onTraktSettings) {
                        Icon(Icons.Default.Settings, "Trakt Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            val tabs = listOf(LibraryTab.FAVOURITES, LibraryTab.WATCHLIST, LibraryTab.WATCHED)
            val tabLabels = listOf("Favourites", "Watchlist", "Watched")
            TabRow(
                selectedTabIndex = tabs.indexOf(state.selectedTab),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tabLabels[index]) },
                    )
                }
            }

            val items = when (state.selectedTab) {
                LibraryTab.FAVOURITES -> state.favourites
                LibraryTab.WATCHLIST -> state.watchlist
                LibraryTab.WATCHED -> state.watched
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when (state.selectedTab) {
                            LibraryTab.FAVOURITES -> "No favourites yet"
                            LibraryTab.WATCHLIST -> "Your watchlist is empty"
                            LibraryTab.WATCHED -> "Nothing watched yet"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items, key = { it.imdbId }) { item ->
                        LibraryItemCard(
                            item = item,
                            onClick = { onItemClick(item.imdbId, item.contentType) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
    ) {
        AsyncImage(
            model = item.posterUrl.ifBlank { null },
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.year.isNotBlank()) {
            Text(
                text = item.year,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
