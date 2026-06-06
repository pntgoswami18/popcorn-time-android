package com.popcorntime.android.ui.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.Season
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.domain.model.seasons
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
)

@HiltViewModel
class ShowDetailViewModel @Inject constructor(
    private val repository: ShowRepository,
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
            repository.toggleWatched(imdbId)
            _uiState.update { it.copy(isWatched = !it.isWatched) }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            repository.toggleBookmarked(imdbId)
            _uiState.update { it.copy(isBookmarked = !it.isBookmarked) }
        }
    }
}
