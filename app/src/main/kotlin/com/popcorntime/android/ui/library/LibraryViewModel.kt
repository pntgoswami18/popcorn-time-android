package com.popcorntime.android.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab { FAVOURITES, WATCHLIST, WATCHED, DOWNLOADS }

data class LibraryUiState(
    val favourites: List<LibraryItem> = emptyList(),
    val watchlist: List<LibraryItem> = emptyList(),
    val watched: List<LibraryItem> = emptyList(),
    val isTraktConnected: Boolean = false,
    val selectedTab: LibraryTab = LibraryTab.FAVOURITES,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeFavourites(),
                repository.observeWatchlist(),
                repository.observeWatched(),
                repository.isTraktConnected(),
            ) { favs, watchlist, watched, traktConnected ->
                Triple(Triple(favs, watchlist, watched), traktConnected, Unit)
            }.collect { (data, traktConnected, _) ->
                val (favs, watchlist, watched) = data
                _uiState.update { it.copy(
                    favourites = favs,
                    watchlist = watchlist,
                    watched = watched,
                    isTraktConnected = traktConnected,
                ) }
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncError = null) }
            try {
                repository.syncFromTrakt()
                _uiState.update { it.copy(isSyncing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, syncError = e.message) }
            }
        }
    }
}
