package com.popcorntime.android.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.preferences.BrowserPrefsStore
import com.popcorntime.android.domain.model.ALL_GENRES
import com.popcorntime.android.domain.model.ALL_QUALITIES
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.MovieFilter
import com.popcorntime.android.domain.model.SortOption
import com.popcorntime.android.domain.repository.LibraryRepository
import com.popcorntime.android.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val libraryRepository: LibraryRepository,
    private val browserPrefsStore: BrowserPrefsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieBrowserUiState())
    val uiState: StateFlow<MovieBrowserUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null

    val watchedIds: StateFlow<Set<String>> = libraryRepository.observeWatched()
        .map { items -> items.map { it.imdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val hideWatched: StateFlow<Boolean> = browserPrefsStore.hideWatchedMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val ratingOrder = listOf("G", "PG", "PG-13", "R", "NC-17")

    val maxRating: StateFlow<String> = browserPrefsStore.maxContentRating
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun applyFilters(movies: List<Movie>): List<Movie> {
        var result = movies
        if (hideWatched.value) {
            result = result.filter { it.imdbId !in watchedIds.value }
        }
        val max = maxRating.value.ifEmpty { return result }
        val maxIdx = ratingOrder.indexOf(max).takeIf { it >= 0 } ?: return result
        return result.filter { m ->
            val idx = ratingOrder.indexOf(m.certification)
            idx < 0 || idx <= maxIdx
        }
    }

    init {
        loadMovies(reset = true)
        observeWatchedAndBookmarked()
        // Re-apply filters when filter prefs change
        viewModelScope.launch {
            combine(hideWatched, maxRating, watchedIds) { _, _, _ -> Unit }
                .drop(1)   // skip the initial emission before movies are loaded
                .collect {
                    loadMovies(reset = true)
                }
        }
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

        // Set loading flag synchronously BEFORE launching the coroutine to prevent TOCTOU race
        _uiState.update {
            if (reset) it.copy(isLoading = true, error = null, movies = emptyList())
            else it.copy(isLoadingMore = true, error = null)
        }

        viewModelScope.launch {

            repository.getMovies(filter).fold(
                onSuccess = { newMovies ->
                    _uiState.update {
                        val rawMovies = if (reset) newMovies else it.movies + newMovies
                        it.copy(
                            movies = applyFilters(rawMovies),
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            // Butter returns ~50/page but client-side quality/rating filters
                            // can shrink a page; only an empty page means the end.
                            hasMore = newMovies.isNotEmpty(),
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
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            loadMovies(reset = true)
        }
    }

    fun onGenreSelect(genre: String) {
        searchDebounceJob?.cancel()
        _uiState.update { it.copy(selectedGenre = genre) }
        loadMovies(reset = true)
    }

    fun onSortSelect(sort: SortOption) {
        searchDebounceJob?.cancel()
        _uiState.update { it.copy(selectedSort = sort) }
        loadMovies(reset = true)
    }

    fun onQualitySelect(quality: String) {
        searchDebounceJob?.cancel()
        _uiState.update { it.copy(selectedQuality = quality) }
        loadMovies(reset = true)
    }

    fun toggleBookmark(imdbId: String) {
        viewModelScope.launch {
            val movie = _uiState.value.movies.find { it.imdbId == imdbId }
                ?: return@launch  // can't build metadata without the movie
            val metadata = LibraryItem(
                imdbId = movie.imdbId,
                title = movie.title,
                posterUrl = movie.posterUrl,
                year = movie.year.toString(),
                contentType = LibraryContentType.MOVIE,
                addedAt = System.currentTimeMillis(),
            )
            libraryRepository.toggleFavourite(imdbId, metadata)
        }
    }

    fun pickRandom(): String? = _uiState.value.movies.randomOrNull()?.imdbId

    private fun observeWatchedAndBookmarked() {
        // Single source of truth: libraryRepository via the top-level watchedIds StateFlow
        viewModelScope.launch {
            watchedIds.collect { ids ->
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
