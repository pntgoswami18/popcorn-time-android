package com.popcorntime.android.ui.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.repository.LibraryRepository
import com.popcorntime.android.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isWatched: Boolean = false,
    val isBookmarked: Boolean = false,
    val isInWatchlist: Boolean = false,
    val selectedQuality: String = "1080p",
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val libraryRepository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Movie is passed via the browse list (stored in a shared holder) in Phase 1.
    // In Phase 3 this will be backed by TMDB detail fetch.
    private val imdbId: String = checkNotNull(savedStateHandle["imdbId"])

    private val _uiState = MutableStateFlow(MovieDetailUiState(isLoading = true))
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    fun loadMovie(movie: Movie) {
        viewModelScope.launch {
            val watched = repository.isWatched(movie.imdbId)
            val bookmarked = repository.isBookmarked(movie.imdbId)
            val inWatchlist = libraryRepository.isInWatchlist(movie.imdbId)
            val bestQuality = movie.torrents.keys
                .sortedWith(compareByDescending { qualityRank(it) })
                .firstOrNull() ?: "1080p"
            _uiState.update {
                it.copy(
                    movie = movie,
                    isLoading = false,
                    isWatched = watched,
                    isBookmarked = bookmarked,
                    isInWatchlist = inWatchlist,
                    selectedQuality = bestQuality,
                )
            }
        }
    }

    /** Called when navigating to detail screen via imdbId (Phase 3 TMDB path). */
    fun loadMovie(imdbId: String) {
        // Look up from the shared movie cache (populated by the browse screen)
        val cached = MovieCache.get(imdbId)
        if (cached != null) { loadMovie(cached); return }
        _uiState.update { it.copy(isLoading = false, error = "Movie not found") }
    }

    fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun toggleWatched() {
        viewModelScope.launch {
            repository.toggleWatched(imdbId)
            _uiState.update { it.copy(isWatched = !it.isWatched) }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val movie = _uiState.value.movie ?: return@launch
            libraryRepository.toggleFavourite(imdbId, movie.toLibraryItem())
            _uiState.update { it.copy(isBookmarked = !it.isBookmarked) }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val movie = _uiState.value.movie ?: return@launch
            if (_uiState.value.isInWatchlist) {
                libraryRepository.removeFromWatchlist(imdbId)
            } else {
                libraryRepository.addToWatchlist(imdbId, movie.toLibraryItem())
            }
            _uiState.update { it.copy(isInWatchlist = !it.isInWatchlist) }
        }
    }

    private fun qualityRank(quality: String) = when (quality) {
        "2160p" -> 4; "1080p" -> 3; "720p" -> 2; "3D" -> 1; else -> 0
    }

    private fun Movie.toLibraryItem() = LibraryItem(
        imdbId = imdbId,
        title = title,
        posterUrl = posterUrl,
        year = year.toString(),
        contentType = LibraryContentType.MOVIE,
        addedAt = System.currentTimeMillis(),
    )
}

/** Simple in-process cache so the browse list can pass Movie objects to the detail screen
 *  without serialisation overhead. Replaced by a proper DB-backed cache in Phase 3. */
object MovieCache {
    private val map = mutableMapOf<String, Movie>()
    fun put(movie: Movie) { map[movie.imdbId] = movie }
    fun get(imdbId: String): Movie? = map[imdbId]
}
