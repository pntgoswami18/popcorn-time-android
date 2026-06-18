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
            // Client-side filters (quality/rating/watched) can shrink a server page to
            // nothing while the server still has more pages. So: hasMore is based on the
            // RAW page size, and when a page filters to empty we keep auto-fetching the
            // next page (bounded) so the user isn't left staring at an empty screen.
            var currentPage = page
            var fetchedMovies = emptyList<Movie>()
            var hasMore = true
            var attempts = 0
            var failure: Throwable? = null
            while (true) {
                attempts++
                val result = repository.getMovies(filter.copy(page = currentPage))
                val moviePage = result.getOrNull()
                if (moviePage == null) {
                    failure = result.exceptionOrNull()
                    break
                }
                fetchedMovies = fetchedMovies + moviePage.movies
                // Only an empty RAW page means the end of the catalogue.
                hasMore = moviePage.rawCount > 0
                if (!shouldAutoFetchNextPage(
                        filteredPageEmpty = applyFilters(fetchedMovies).isEmpty(),
                        rawPageCount = moviePage.rawCount,
                        attempts = attempts,
                    )
                ) {
                    break
                }
                currentPage++
            }
            val error = failure
            if (error != null && attempts == 1) {
                // First fetch failed — surface the error.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = error.message ?: "Failed to load movies",
                    )
                }
            } else {
                // Success, or an auto-fetch follow-up failed after we already got data —
                // show what we have and let the next scroll retry.
                val lastFetchedPage = if (error != null) currentPage - 1 else currentPage
                _uiState.update {
                    val rawMovies = if (reset) fetchedMovies else it.movies + fetchedMovies
                    it.copy(
                        movies = applyFilters(rawMovies),
                        isLoading = false,
                        isLoadingMore = false,
                        currentPage = lastFetchedPage,
                        hasMore = hasMore,
                        filter = filter.copy(page = lastFetchedPage),
                    )
                }
            }
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

    companion object {
        /** Bound on consecutive auto-fetches when client-side filters empty out pages. */
        internal const val MAX_AUTO_FETCH_PAGES = 5
    }

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

/**
 * Decides whether to auto-fetch the next server page during a single load.
 * True while client-side filters have emptied everything fetched so far, the
 * server still returned a non-empty raw page, and we're under the fetch bound.
 * Pure and JVM-testable.
 */
internal fun shouldAutoFetchNextPage(
    filteredPageEmpty: Boolean,
    rawPageCount: Int,
    attempts: Int,
    maxAttempts: Int = MovieBrowserViewModel.MAX_AUTO_FETCH_PAGES,
): Boolean = filteredPageEmpty && rawPageCount > 0 && attempts < maxAttempts
