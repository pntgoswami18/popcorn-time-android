package com.popcorntime.android.ui.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.model.Season
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.domain.model.seasons
import com.popcorntime.android.domain.repository.LibraryRepository
import com.popcorntime.android.domain.repository.ShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShowDetailUiState(
    val show: Show? = null,
    val seasons: List<Season> = emptyList(),
    val selectedSeason: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isWatched: Boolean = false,
    val isBookmarked: Boolean = false,
    val isInWatchlist: Boolean = false,
)

@HiltViewModel
class ShowDetailViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val libraryRepository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val imdbId: String = checkNotNull(savedStateHandle["imdbId"])
    private val typeStr: String = savedStateHandle["contentType"] ?: "show"
    private val contentType = if (typeStr == "anime") ContentType.ANIME else ContentType.SHOW

    private val _uiState = MutableStateFlow(ShowDetailUiState())
    val uiState: StateFlow<ShowDetailUiState> = _uiState.asStateFlow()

    init { loadDetail() }

    private fun loadDetail() {
        // Use cache first for instant display, then fetch full detail (with episodes)
        val cached = ShowCache.get(imdbId)
        if (cached != null) {
            val seasons = cached.seasons()
            _uiState.update {
                it.copy(show = cached, seasons = seasons,
                    selectedSeason = seasons.firstOrNull()?.number ?: 1, isLoading = false)
            }
        }

        viewModelScope.launch {
            // Load watchlist, favourite, and watched state
            val inWatchlist = libraryRepository.isInWatchlist(imdbId)
            val isFavourited = libraryRepository.isFavourited(imdbId)
            val isWatched = libraryRepository.isWatched(imdbId)
            _uiState.update { it.copy(isInWatchlist = inWatchlist, isBookmarked = isFavourited, isWatched = isWatched) }

            repository.getShowDetail(imdbId, contentType).fold(
                onSuccess = { show ->
                    ShowCache.put(show)
                    val seasons = show.seasons()
                    _uiState.update {
                        it.copy(show = show, seasons = seasons,
                            selectedSeason = seasons.firstOrNull()?.number ?: 1, isLoading = false)
                    }
                },
                onFailure = { e ->
                    if (_uiState.value.show == null) {
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    // If we already have cached data, don't show error
                },
            )
        }
    }

    fun selectSeason(number: Int) {
        _uiState.update { it.copy(selectedSeason = number) }
    }

    fun toggleWatched() {
        viewModelScope.launch {
            val show = _uiState.value.show ?: return@launch
            if (_uiState.value.isWatched) {
                libraryRepository.unmarkWatched(imdbId)
            } else {
                libraryRepository.markWatched(imdbId, show.toLibraryItem())
            }
            _uiState.update { it.copy(isWatched = !it.isWatched) }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val show = _uiState.value.show ?: return@launch
            libraryRepository.toggleFavourite(imdbId, show.toLibraryItem())
            _uiState.update { it.copy(isBookmarked = !it.isBookmarked) }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            val show = _uiState.value.show ?: return@launch
            if (_uiState.value.isInWatchlist) {
                libraryRepository.removeFromWatchlist(imdbId)
            } else {
                libraryRepository.addToWatchlist(imdbId, show.toLibraryItem())
            }
            _uiState.update { it.copy(isInWatchlist = !it.isInWatchlist) }
        }
    }

    private fun Show.toLibraryItem() = LibraryItem(
        imdbId = imdbId,
        title = title,
        posterUrl = posterUrl,
        year = year,
        contentType = if (contentType == ContentType.ANIME) LibraryContentType.ANIME else LibraryContentType.SHOW,
        addedAt = System.currentTimeMillis(),
    )
}
