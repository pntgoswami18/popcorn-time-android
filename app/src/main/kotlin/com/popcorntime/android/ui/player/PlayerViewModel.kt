package com.popcorntime.android.ui.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.cast.CastManager
import com.popcorntime.android.data.cast.DlnaRenderer
import com.popcorntime.android.data.cast.KodiPrefsStore
import com.popcorntime.android.data.remote.PlaybackController
import com.popcorntime.android.data.remote.PlaybackQueue
import com.popcorntime.android.data.subtitles.Subtitle
import com.popcorntime.android.data.subtitles.SubtitleService
import com.popcorntime.android.data.torrent.TorrentEngine
import com.popcorntime.android.data.torrent.TorrentService
import com.popcorntime.android.domain.model.CastState
import com.popcorntime.android.domain.model.ContentType
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
    val playbackController: PlaybackController,
    val playbackQueue: PlaybackQueue,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val imdbId: String = savedStateHandle["imdbId"] ?: ""
    val quality: String = savedStateHandle["quality"] ?: ""
    val season: Int = savedStateHandle["season"] ?: -1
    val episode: Int = savedStateHandle["episode"] ?: -1
    val contentTypeStr: String = savedStateHandle["contentType"] ?: "movie"

    // Mutable backing fields for the currently playing item (updated on queue advance)
    private var currentImdbId: String = imdbId
    private var currentQuality: String = quality
    private var currentSeason: Int? = if (season == -1) null else season
    private var currentEpisode: Int? = if (episode == -1) null else episode

    val streamState: StateFlow<StreamState> = torrentEngine.state

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        castManager.chromeCaster.registerSessionListener()
        if (imdbId.isBlank()) {
            torrentEngine.setError("Missing content identifier")
        } else {
            startStream()
        }
        loadSubtitles(currentImdbId)
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
        val movie = MovieCache.get(currentImdbId)
        if (movie != null) {
            val torrent = movie.torrents[currentQuality]
                ?: movie.torrents.values.maxByOrNull { it.seeds }
                ?: run {
                    torrentEngine.setError("No torrent found for quality: $currentQuality")
                    return
                }
            context.startForegroundService(Intent(context, TorrentService::class.java))
            torrentEngine.startStream(torrent)  // non-suspend; spawns its own coroutine internally
            return
        }

        // Fall back to show cache for episode torrents
        val show = ShowCache.get(currentImdbId)
        if (show == null) {
            torrentEngine.setError("Content not found in cache")
            return
        }
        val ep = show.episodes.firstOrNull { it.season == currentSeason && it.episode == currentEpisode }
        if (ep == null) {
            torrentEngine.setError("Episode S${currentSeason}E${currentEpisode} not found")
            return
        }
        val episodeTorrent = ep.torrents[currentQuality] ?: ep.torrents.values.firstOrNull()
        if (episodeTorrent == null) {
            torrentEngine.setError("No torrent for this episode")
            return
        }
        val torrent = Torrent(
            url = episodeTorrent.url,
            magnet = "",
            quality = currentQuality,
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

    private fun loadSubtitles(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSubtitles = true) }
            val results = subtitleService.searchSubtitles(id)
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
        val title = MovieCache.get(currentImdbId)?.title
            ?: ShowCache.get(currentImdbId)?.title
            ?: currentImdbId
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
            val metadata = buildLibraryMetadata()
            if (metadata != null) libraryRepository.markWatched(currentImdbId, metadata)
        }
        // Auto-advance to the next item in the queue, if any
        val next = playbackQueue.dequeue()
        if (next != null) {
            currentImdbId = next.imdbId
            currentQuality = next.quality
            currentSeason = next.season
            currentEpisode = next.episode
            // Clear stale subtitle state before loading new content
            _uiState.update { it.copy(selectedSubtitle = null, subtitleUrl = null) }
            loadSubtitles(next.imdbId)
            val torrent = Torrent(
                url = next.magnet,
                magnet = next.magnet,
                quality = next.quality,
                type = when (next.contentType) {
                    LibraryContentType.MOVIE -> "bluray"
                    else -> "show"
                },
                size = 0L,
                fileSize = "",
                seeds = 0,
                peers = 0,
                hash = "",
            )
            context.startForegroundService(Intent(context, TorrentService::class.java))
            torrentEngine.startStream(torrent)
        }
    }

    private fun buildLibraryMetadata(): LibraryItem? {
        val movie = MovieCache.get(currentImdbId)
        if (movie != null) {
            return LibraryItem(
                imdbId = currentImdbId,
                title = movie.title,
                posterUrl = movie.posterUrl,
                year = movie.year.toString(),
                contentType = LibraryContentType.MOVIE,
                addedAt = System.currentTimeMillis(),
            )
        }
        val show = ShowCache.get(currentImdbId)
        if (show != null) {
            return LibraryItem(
                imdbId = currentImdbId,
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
