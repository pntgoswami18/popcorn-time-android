package com.popcorntime.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.sources.TorrentSourcePrefs
import com.popcorntime.android.domain.model.TorrentSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceSettingsUiState(
    val movieSource: TorrentSource = TorrentSource.YTS,
    val showSource: TorrentSource = TorrentSource.EZTV,
    val jackettUrl: String = "",
    val jackettApiKey: String = "",
    val isSaved: Boolean = false,
)

@HiltViewModel
class SourceSettingsViewModel @Inject constructor(
    private val sourcePrefs: TorrentSourcePrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourceSettingsUiState())
    val uiState: StateFlow<SourceSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val movieSource = sourcePrefs.getMovieSource()
            val showSource = sourcePrefs.getShowSource()
            val jackettUrl = sourcePrefs.getJackettUrl()
            val jackettApiKey = sourcePrefs.getJackettApiKey()
            _uiState.update {
                it.copy(
                    movieSource = movieSource,
                    showSource = showSource,
                    jackettUrl = jackettUrl,
                    jackettApiKey = jackettApiKey,
                )
            }
        }
    }

    fun setMovieSource(source: TorrentSource) {
        viewModelScope.launch {
            sourcePrefs.setMovieSource(source)
            _uiState.update { it.copy(movieSource = source, isSaved = false) }
        }
    }

    fun setShowSource(source: TorrentSource) {
        viewModelScope.launch {
            sourcePrefs.setShowSource(source)
            _uiState.update { it.copy(showSource = source, isSaved = false) }
        }
    }

    fun saveJackettConfig(url: String, apiKey: String) {
        viewModelScope.launch {
            sourcePrefs.saveJackettConfig(url, apiKey)
            _uiState.update { it.copy(jackettUrl = url, jackettApiKey = apiKey, isSaved = true) }
            viewModelScope.launch {
                delay(2_000)
                _uiState.update { it.copy(isSaved = false) }
            }
        }
    }
}
