package com.popcorntime.android.ui.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.cast.CastManager
import com.popcorntime.android.data.cast.DlnaRenderer
import com.popcorntime.android.data.cast.KodiPrefsStore
import com.popcorntime.android.data.subtitles.Subtitle
import com.popcorntime.android.data.subtitles.SubtitleService
import com.popcorntime.android.data.torrent.TorrentEngine
import com.popcorntime.android.data.torrent.TorrentService
import com.popcorntime.android.domain.model.CastState
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.domain.model.Torrent
import com.popcorntime.android.domain.repository.LibraryRepository
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
    val showCastSheet: Boolean = false,
    val castState: CastState = CastState.Idle,
    val dlnaRenderers: List<DlnaRenderer> = emptyList(),
    val kodiAddress: Pair<String, Int> = Pair("", 8080),
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val torrentEngine: TorrentEngine,
    private val subtitleService: SubtitleService,
    private val libraryRepository: LibraryRepository,
    private val castManager: CastManager,
    private val kodiPrefsStore: KodiPrefsStore,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val imdbId: String = checkNotNull(savedStateHandle["imdbId"])
    val quality: String = checkNotNull(savedStateHandle["quality"])
    val season: Int = savedStateHandle["season"] ?: -1
    val episode: Int = savedStateHandle["episode"] ?: -1
    val contentTypeStr: String = savedStateHandle["contentType"] ?: "movie"

    val streamState: StateFlow<StreamState> = torrentEngine.state

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        castManager.chromeCaster.registerSessionListener()
        startStream()
        loadSubtitles()
        // Load saved Kodi address
        viewModelScope.launch {
            kodiPrefsStore.observeAddress().collect { addr ->
                _uiState.update { it.copy(kodiAddress = addr) }
            }
        }
        // Observe cast state
        viewModelScope.launch {
            castManager.castState.collect { cs ->
                _uiState.update { it.copy(castState = cs) }
            }
        }
        // Observe DLNA renderers (only when cast sheet is open)
        viewModelScope.launch {
            castManager.dlnaDiscovery.renderers.collect { renderers ->
                _uiState.update { it.copy(dlnaRenderers = renderers) }
            }
        }
    }

    private fun startStream() {
        // Try movie cache first
        val movie = MovieCache.get(imdbId)
        if (movie != null) {
            val torrent = movie.torrents[quality]
                ?: movie.torrents.values.maxByOrNull { it.seeds }
                ?: run {
                    torrentEngine.setError("No torrent found for quality: $quality")
                    return
                }
            context.startForegroundService(Intent(context, TorrentService::class.java))
            torrentEngine.startStream(torrent)  // non-suspend; spawns its own coroutine internally
            return
        }

        // Fall back to show cache for episode torrents
        val show = ShowCache.get(imdbId)
        if (show == null) {
            torrentEngine.setError("Content not found in cache")
            return
        }
        val ep = show.episodes.firstOrNull { it.season == season && it.episode == episode }
        if (ep == null) {
            torrentEngine.setError("Episode S${season}E${episode} not found")
            return
        }
        val episodeTorrent = ep.torrents[quality] ?: ep.torrents.values.firstOrNull()
        if (episodeTorrent == null) {
            torrentEngine.setError("No torrent for this episode")
            return
        }
        val torrent = Torrent(
            url = episodeTorrent.url,
            magnet = "",
            quality = quality,
            type = "show",
            size = 0L,
            fileSize = "",
            seeds = episodeTorrent.seeds,
            peers = episodeTorrent.peers,
            hash = "",
        )
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

    fun toggleCastSheet() {
        val opening = !_uiState.value.showCastSheet
        _uiState.update { it.copy(showCastSheet = opening) }
        if (opening) castManager.dlnaDiscovery.startDiscovery()
        else castManager.dlnaDiscovery.stopDiscovery()
    }

    fun castToChromecast() {
        val streamUrl = (streamState.value as? StreamState.Ready)?.streamUrl ?: return
        val title = MovieCache.get(imdbId)?.title
            ?: ShowCache.get(imdbId)?.title
            ?: imdbId
        castManager.castToChromecast(streamUrl, title)
        _uiState.update { it.copy(showCastSheet = false) }
    }

    fun castToExternalPlayer() {
        val streamUrl = (streamState.value as? StreamState.Ready)?.streamUrl ?: return
        castManager.castToExternalPlayer(streamUrl)
        _uiState.update { it.copy(showCastSheet = false) }
    }

    fun castToKodi(host: String, port: Int) {
        val streamUrl = (streamState.value as? StreamState.Ready)?.streamUrl ?: return
        viewModelScope.launch { kodiPrefsStore.saveAddress(host, port) }
        castManager.castToKodi(streamUrl, host, port)
        _uiState.update { it.copy(showCastSheet = false) }
    }

    fun castToDlna(renderer: DlnaRenderer) {
        val streamUrl = (streamState.value as? StreamState.Ready)?.streamUrl ?: return
        castManager.castToDlna(streamUrl, renderer)
        _uiState.update { it.copy(showCastSheet = false) }
    }

    fun disconnectCast() {
        castManager.disconnect()
    }

    fun stopStream() {
        torrentEngine.stopCurrent()
        context.stopService(Intent(context, TorrentService::class.java))
        castManager.disconnect()
        castManager.dlnaDiscovery.stopDiscovery()
    }

    fun onPlaybackCompleted() {
        viewModelScope.launch {
            val metadata = buildLibraryMetadata() ?: return@launch
            libraryRepository.markWatched(imdbId, metadata)
        }
    }

    private fun buildLibraryMetadata(): LibraryItem? {
        val movie = MovieCache.get(imdbId)
        if (movie != null) {
            return LibraryItem(
                imdbId = imdbId,
                title = movie.title,
                posterUrl = movie.posterUrl,
                year = movie.year.toString(),
                contentType = LibraryContentType.MOVIE,
                addedAt = System.currentTimeMillis(),
            )
        }
        val show = ShowCache.get(imdbId)
        if (show != null) {
            return LibraryItem(
                imdbId = imdbId,
                title = show.title,
                posterUrl = show.posterUrl,
                year = show.year,
                contentType = if (contentTypeStr == "anime") LibraryContentType.ANIME else LibraryContentType.SHOW,
                addedAt = System.currentTimeMillis(),
            )
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        // PlayerScreen's DisposableEffect calls stopStream() on composition disposal.
        // Here we only clean up cast-specific resources that outlive the screen.
        castManager.chromeCaster.unregisterSessionListener()
        castManager.dlnaDiscovery.stopDiscovery()
        castManager.disconnect()
    }
}
