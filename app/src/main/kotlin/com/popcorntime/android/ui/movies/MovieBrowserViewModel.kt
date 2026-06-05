package com.popcorntime.android.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.domain.model.ALL_GENRES
import com.popcorntime.android.domain.model.ALL_QUALITIES
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.MovieFilter
import com.popcorntime.android.domain.model.SortOption
import com.popcorntime.android.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieBrowserUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val filter: MovieFilter = MovieFilter(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val searchQuery: String = "",
    val selectedGenre: String = "All",
    val selectedSort: SortOption = SortOption.LAST_ADDED,
    val selectedQuality: String = "All",
    val watchedIds: Set<String> = emptySet(),
    val bookmarkedIds: Set<String> = emptySet(),
)

@HiltViewModel
class MovieBrowserViewModel @Inject constructor(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieBrowserUiState())
    val uiState: StateFlow<MovieBrowserUiState> = _uiState.asStateFlow()

    init {
        loadMovies(reset = true)
        observeWatchedAndBookmarked()
    }

    fun loadMovies(reset: Boolean = false) {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore) return
        if (!reset && !state.hasMore) return

        val page = if (reset) 1 else state.currentPage + 1
        val filter = state.filter.copy(
            page = page,
            genre = state.selectedGenre,
            sortBy = state.selectedSort.apiValue,
            quality = state.selectedQuality,
            queryTerm = state.searchQuery,
        )

        viewModelScope.launch {
            _uiState.update {
                if (reset) it.copy(isLoading = true, error = null, movies = emptyList())
                else it.copy(isLoadingMore = true, error = null)
            }

            repository.getMovies(filter).fold(
                onSuccess = { newMovies ->
                    _uiState.update {
                        it.copy(
                            movies = if (reset) newMovies else it.movies + newMovies,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            hasMore = newMovies.size >= 20,
                            filter = filter,
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = e.message ?: "Failed to load movies",
                        )
                    }
                },
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadMovies(reset = true)
    }

    fun onGenreSelect(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
        loadMovies(reset = true)
    }

    fun onSortSelect(sort: SortOption) {
        _uiState.update { it.copy(selectedSort = sort) }
        loadMovies(reset = true)
    }

    fun onQualitySelect(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
        loadMovies(reset = true)
    }

    fun toggleBookmark(imdbId: String) {
        viewModelScope.launch { repository.toggleBookmarked(imdbId) }
    }

    private fun observeWatchedAndBookmarked() {
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
