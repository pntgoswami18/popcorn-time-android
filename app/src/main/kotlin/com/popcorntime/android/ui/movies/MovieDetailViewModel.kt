package com.popcorntime.android.ui.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.torrent.DownloadManager
import com.popcorntime.android.data.torrent.DownloadStats
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.model.Movie
import com.popcorntime.android.domain.model.Torrent
import com.popcorntime.android.domain.repository.LibraryRepository
import com.popcorntime.android.domain.repository.MovieRepository
import com.popcorntime.android.ui.settings.resolvePlayableVideoUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Download status of this movie, derived from DownloadManager state. */
sealed interface MovieDownloadState {
    data object NotDownloaded : MovieDownloadState

    /** Entity exists and is incomplete, but it is not the actively-downloading item. */
    data object Queued : MovieDownloadState

    /** This is the active download but live stats have not arrived yet. */
    data object Starting : MovieDownloadState

    data class Downloading(val stats: DownloadStats) : MovieDownloadState

    /** Download finished; [localUri] is the playable file URI if one was resolved. */
    data class Downloaded(val localUri: String?) : MovieDownloadState
}

data class MovieDetailUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isWatched: Boolean = false,
    val isBookmarked: Boolean = false,
    val isInWatchlist: Boolean = false,
    val selectedQuality: String = "1080p",
    val downloadState: MovieDownloadState = MovieDownloadState.NotDownloaded,
    /** One-shot message shown in a snackbar, e.g. when a download is rejected. */
    val transientMessage: String? = null,
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val libraryRepository: LibraryRepository,
    private val downloadManager: DownloadManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Movie is passed via the browse list (stored in a shared holder) in Phase 1.
    // In Phase 3 this will be backed by TMDB detail fetch.
    private val imdbId: String = checkNotNull(savedStateHandle["imdbId"])

    private val _uiState = MutableStateFlow(MovieDetailUiState(isLoading = true))
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        // Keep the per-movie download state in sync with DownloadManager.
        viewModelScope.launch {
            combine(
                downloadManager.downloads,
                downloadManager.activeDownloadStats,
                downloadManager.activeImdbId,
            ) { downloads, stats, activeId ->
                val entity = downloads.find { it.imdbId == imdbId }
                when {
                    entity == null -> MovieDownloadState.NotDownloaded
                    entity.completedAt != null ->
                        MovieDownloadState.Downloaded(resolvePlayableVideoUri(entity.filePath))
                    stats?.imdbId == imdbId -> MovieDownloadState.Downloading(stats)
                    activeId == imdbId -> MovieDownloadState.Starting
                    else -> MovieDownloadState.Queued
                }
            }
                .flowOn(Dispatchers.IO) // resolvePlayableVideoUri walks the filesystem
                .collect { downloadState ->
                    _uiState.update { it.copy(downloadState = downloadState) }
                }
        }
    }

    fun loadMovie(movie: Movie) {
        viewModelScope.launch {
            val watched = libraryRepository.isWatched(movie.imdbId)
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
        // Cache miss — fetch from API (covers LibraryScreen → detail navigation)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getMovieDetail(imdbId)
            result.fold(
                onSuccess = { movie ->
                    MovieCache.put(movie)
                    loadMovie(movie)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load movie") }
                },
            )
        }
    }

    fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun toggleWatched() {
        viewModelScope.launch {
            val movie = _uiState.value.movie ?: return@launch
            val wasWatched = _uiState.value.isWatched
            try {
                if (wasWatched) libraryRepository.unmarkWatched(imdbId)
                else libraryRepository.markWatched(imdbId, movie.toLibraryItem())
                _uiState.update { it.copy(isWatched = !wasWatched) }
            } catch (e: Exception) {
                _uiState.update { it.copy(transientMessage = "Failed to update watched status") }
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val movie = _uiState.value.movie ?: return@launch
            val wasBookmarked = _uiState.value.isBookmarked
            try {
                libraryRepository.toggleFavourite(imdbId, movie.toLibraryItem())
                _uiState.update { it.copy(isBookmarked = !wasBookmarked) }
            } catch (e: Exception) {
                _uiState.update { it.copy(transientMessage = "Failed to update bookmark") }
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val movie = _uiState.value.movie ?: return@launch
            val wasInWatchlist = _uiState.value.isInWatchlist
            try {
                if (wasInWatchlist) libraryRepository.removeFromWatchlist(imdbId)
                else libraryRepository.addToWatchlist(imdbId, movie.toLibraryItem())
                _uiState.update { it.copy(isInWatchlist = !wasInWatchlist) }
            } catch (e: Exception) {
                _uiState.update { it.copy(transientMessage = "Failed to update watchlist") }
            }
        }
    }

    fun startDownload(torrent: Torrent) {
        val movie = _uiState.value.movie ?: return
        val started = downloadManager.startDownload(
            movie.imdbId,
            movie.title,
            torrent.magnet.ifBlank { torrent.url },
            torrent.quality,
        )
        if (!started) {
            _uiState.update { it.copy(transientMessage = "Another download is already in progress") }
        }
    }

    /** Called by the UI after the snackbar for [MovieDetailUiState.transientMessage] is shown. */
    fun consumeTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    fun cancelDownload() {
        downloadManager.cancelDownload(imdbId)
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
    private val map = java.util.concurrent.ConcurrentHashMap<String, Movie>()
    fun put(movie: Movie) { map[movie.imdbId] = movie }
    fun get(imdbId: String): Movie? = map[imdbId]
}
