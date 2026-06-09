package com.popcorntime.android.ui.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.domain.model.ShowFilter
import com.popcorntime.android.domain.repository.LibraryRepository
import com.popcorntime.android.domain.repository.ShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShowBrowserUiState(
    val shows: List<Show> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val searchQuery: String = "",
    val selectedGenre: String = "All",
    val selectedSort: String = "trending",
    val watchedIds: Set<String> = emptySet(),
    val bookmarkedIds: Set<String> = emptySet(),
)

@HiltViewModel
class ShowBrowserViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowBrowserUiState())
    val uiState: StateFlow<ShowBrowserUiState> = _uiState.asStateFlow()

    private var contentType: ContentType = ContentType.SHOW
    private var searchDebounceJob: Job? = null
    private var observingState = false

    fun init(type: ContentType) {
        if (contentType == type && _uiState.value.shows.isNotEmpty()) return
        contentType = type
        loadShows(reset = true)
        if (!observingState) {
            observingState = true
            observeState()
        }
    }

    fun loadShows(reset: Boolean = false) {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore) return
        if (!reset && !state.hasMore) return

        val page = if (reset) 1 else state.currentPage + 1
        val filter = ShowFilter(
            page = page,
            genre = state.selectedGenre,
            sortBy = state.selectedSort,
            keywords = state.searchQuery,
            type = contentType,
        )

        // Set loading flag synchronously BEFORE launching the coroutine to prevent TOCTOU race
        _uiState.update {
            if (reset) it.copy(isLoading = true, error = null, shows = emptyList())
            else it.copy(isLoadingMore = true, error = null)
        }

        viewModelScope.launch {
            repository.getShows(filter).fold(
                onSuccess = { newShows ->
                    _uiState.update {
                        it.copy(
                            shows = if (reset) newShows else it.shows + newShows,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            hasMore = newShows.isNotEmpty(),
                        )
                    }
                },
                onFailure = { e ->
                    val msg = e.message ?: ""
                    if (msg.contains("404")) {
                        _uiState.update {
                            it.copy(isLoading = false, isLoadingMore = false, hasMore = false)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, isLoadingMore = false,
                                error = msg.ifBlank { "Failed to load" })
                        }
                    }
                },
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            loadShows(reset = true)
        }
    }

    fun onGenreSelect(genre: String) {
        searchDebounceJob?.cancel()
        _uiState.update { it.copy(selectedGenre = genre) }
        loadShows(reset = true)
    }

    fun onSortSelect(sort: String) {
        searchDebounceJob?.cancel()
        _uiState.update { it.copy(selectedSort = sort) }
        loadShows(reset = true)
    }

    fun toggleBookmark(imdbId: String) {
        viewModelScope.launch {
            val show = _uiState.value.shows.find { it.imdbId == imdbId }
                ?: return@launch  // can't build metadata without the show
            val metadata = LibraryItem(
                imdbId = show.imdbId,
                title = show.title,
                posterUrl = show.posterUrl,
                year = show.year,
                contentType = if (contentType == ContentType.ANIME) LibraryContentType.ANIME else LibraryContentType.SHOW,
                addedAt = System.currentTimeMillis(),
            )
            libraryRepository.toggleFavourite(imdbId, metadata)
        }
    }

    fun pickRandom(): String? = _uiState.value.shows.randomOrNull()?.imdbId

    private fun observeState() {
        viewModelScope.launch {
            repository.observeWatched().collect { ids ->
                _uiState.update { it.copy(watchedIds = ids) }
            }
        }
        viewModelScope.launch {
            libraryRepository.observeFavourites().collect { items ->
                _uiState.update { it.copy(bookmarkedIds = items.map { item -> item.imdbId }.toSet()) }
            }
        }
    }
}
