package com.popcorntime.android.ui.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.subtitles.Subtitle
import com.popcorntime.android.data.subtitles.SubtitleService
import com.popcorntime.android.data.torrent.TorrentEngine
import com.popcorntime.android.data.torrent.TorrentService
import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.domain.model.Torrent
import com.popcorntime.android.ui.movies.MovieCache
import com.popcorntime.android.ui.shows.ShowCache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val subtitles: List<Subtitle> = emptyList(),
    val selectedSubtitle: Subtitle? = null,
    val subtitleUrl: String? = null,
    val isLoadingSubtitles: Boolean = false,
    val showSubtitlePicker: Boolean = false,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val torrentEngine: TorrentEngine,
    private val subtitleService: SubtitleService,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val imdbId: String = checkNotNull(savedStateHandle["imdbId"])
    val quality: String = checkNotNull(savedStateHandle["quality"])
    val season: Int = savedStateHandle["season"] ?: -1
    val episode: Int = savedStateHandle["episode"] ?: -1

    val streamState: StateFlow<StreamState> = torrentEngine.state

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        startStream()
        loadSubtitles()
    }

    private fun startStream() {
        val torrent: Torrent? = run {
            // Try movie cache first
            val movie = MovieCache.get(imdbId)
            if (movie != null) {
                return@run movie.torrents[quality]
                    ?: movie.torrents.values.maxByOrNull { it.seeds }
            }
            // Fall back to show cache for episode torrents
            val show = ShowCache.get(imdbId) ?: return
            val ep = show.episodes.firstOrNull { it.season == season && it.episode == episode }
                ?: return
            val episodeTorrent = ep.torrents[quality]
                ?: ep.torrents.values.maxByOrNull { it.seeds }
                ?: return
            Torrent(
                url = episodeTorrent.url,
                magnet = "",
                quality = quality,
                type = "",
                size = 0L,
                fileSize = "",
                seeds = episodeTorrent.seeds,
                peers = episodeTorrent.peers,
                hash = "",
            )
        } ?: return

        context.startForegroundService(Intent(context, TorrentService::class.java))
        torrentEngine.startStream(torrent)  // non-suspend; spawns its own coroutine internally
    }

    private fun loadSubtitles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSubtitles = true) }
            val results = subtitleService.searchSubtitles(imdbId)
            _uiState.update { it.copy(subtitles = results, isLoadingSubtitles = false) }
        }
    }

    fun selectSubtitle(subtitle: Subtitle?) {
        _uiState.update { it.copy(selectedSubtitle = subtitle, showSubtitlePicker = false, subtitleUrl = null) }
        if (subtitle == null) return
        viewModelScope.launch {
            val url = subtitleService.getDownloadUrl(subtitle.fileId)
            _uiState.update { it.copy(subtitleUrl = url) }
        }
    }

    fun toggleSubtitlePicker() {
        _uiState.update { it.copy(showSubtitlePicker = !it.showSubtitlePicker) }
    }

    fun stopStream() {
        torrentEngine.stopCurrent()
        context.stopService(Intent(context, TorrentService::class.java))
    }

    override fun onCleared() {
        super.onCleared()
        stopStream()
    }
}
