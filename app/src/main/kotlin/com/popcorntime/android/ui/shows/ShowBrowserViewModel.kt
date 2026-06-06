package com.popcorntime.android.ui.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.domain.model.ShowFilter
import com.popcorntime.android.domain.repository.ShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowBrowserUiState())
    val uiState: StateFlow<ShowBrowserUiState> = _uiState.asStateFlow()

    private var contentType: ContentType = ContentType.SHOW

    fun init(type: ContentType) {
        if (contentType == type && _uiState.value.shows.isNotEmpty()) return
        contentType = type
        loadShows(reset = true)
        observeState()
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

        viewModelScope.launch {
            _uiState.update {
                if (reset) it.copy(isLoading = true, error = null, shows = emptyList())
                else it.copy(isLoadingMore = true, error = null)
            }
            repository.getShows(filter).fold(
                onSuccess = { newShows ->
                    _uiState.update {
                        it.copy(
                            shows = if (reset) newShows else it.shows + newShows,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            hasMore = newShows.size >= 20,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, isLoadingMore = false,
                            error = e.message ?: "Failed to load")
                    }
                },
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadShows(reset = true)
    }

    fun onGenreSelect(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
        loadShows(reset = true)
    }

    fun onSortSelect(sort: String) {
        _uiState.update { it.copy(selectedSort = sort) }
        loadShows(reset = true)
    }

    fun toggleBookmark(imdbId: String) {
        viewModelScope.launch { repository.toggleBookmarked(imdbId) }
    }

    private fun observeState() {
        viewModelScope.launch {
            repository.observeWatched().collect { ids ->
                _uiState.update { it.copy(watchedIds = ids) }
            }
        }
        viewModelScope.launch {
            repository.observeBookmarked().collect { ids ->
                _uiState.update { it.copy(bookmarkedIds = ids) }
            }
        }
    }
}
