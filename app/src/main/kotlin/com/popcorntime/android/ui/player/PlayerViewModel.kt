package com.popcorntime.android.ui.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.AspectRatioFrameLayout
import com.popcorntime.android.data.cast.CastManager
import com.popcorntime.android.data.cast.DlnaRenderer
import com.popcorntime.android.data.cast.KodiPrefsStore
import com.popcorntime.android.data.player.PlaybackPositionStore
import com.popcorntime.android.data.remote.PlaybackCommand
import com.popcorntime.android.data.remote.PlaybackController
import com.popcorntime.android.data.remote.PlaybackQueue
import com.popcorntime.android.data.subtitles.Subtitle
import com.popcorntime.android.data.subtitles.SubtitleService
import com.popcorntime.android.data.torrent.TorrentEngine
import com.popcorntime.android.data.torrent.TorrentService
import com.popcorntime.android.data.trakt.TraktScrobbleService
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    val error: String? = null,
    val streamUrl: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val torrentEngine: TorrentEngine,
    private val subtitleService: SubtitleService,
    private val libraryRepository: LibraryRepository,
    private val castManager: CastManager,
    private val kodiPrefsStore: KodiPrefsStore,
    private val playbackPositionStore: PlaybackPositionStore,
    private val traktScrobbleService: TraktScrobbleService,
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
    val localUri: String? = savedStateHandle["localUri"]

    // Mutable backing fields for the currently playing item (updated on queue advance)
    private var currentImdbId: String = imdbId
    private var currentQuality: String = quality
    private var currentSeason: Int? = if (season == -1) null else season
    private var currentEpisode: Int? = if (episode == -1) null else episode
    private var currentContentType: LibraryContentType = LibraryContentType.MOVIE

    val streamState: StateFlow<StreamState> = torrentEngine.state

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Auto-play countdown
    private val _countdownSeconds = MutableStateFlow<Int?>(null)
    val countdownSeconds: StateFlow<Int?> = _countdownSeconds.asStateFlow()
    private var countdownJob: Job? = null
    private var pendingNextItem: com.popcorntime.android.data.remote.QueueItem? = null

    // Aspect ratio / resize mode
    private val _resizeMode = MutableStateFlow(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    // Brightness
    private val _brightness = MutableStateFlow(-1f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    // Position save job
    private var positionSaveJob: Job? = null
    private var resumeJob: Job? = null

    init {
        castManager.chromeCaster.registerSessionListener()
        if (localUri != null) {
            _uiState.update { it.copy(streamUrl = localUri) }
        } else if (imdbId.isBlank()) {
            torrentEngine.setError("Missing content identifier")
        } else {
            startStream()
        }
        if (currentImdbId.isNotBlank()) loadSubtitles(currentImdbId)
        viewModelScope.launch {
            kodiPrefsStore.observeAddress().collect { addr ->
                _uiState.update { it.copy(kodiAddress = addr) }
            }
        }
        viewModelScope.launch {
            castManager.castState.collect { cs ->
                _uiState.update { it.copy(castState = cs) }
            }
        }
        viewModelScope.launch {
            castManager.dlnaDiscovery.renderers.collect { renderers ->
                _uiState.update { it.copy(dlnaRenderers = renderers) }
            }
        }
        // Observe isPlaying for Trakt scrobbling
        viewModelScope.launch {
            playbackController.isPlaying
                .debounce(2_000L)
                .collect { playing ->
                    if (currentImdbId.isNotBlank()) {
                        val progress = currentProgress()
                        if (playing) {
                            runCatching { traktScrobbleService.scrobbleStart(currentImdbId, currentContentType, progress) }
                        } else {
                            runCatching { traktScrobbleService.scrobblePause(currentImdbId, currentContentType, progress) }
                        }
                    }
                }
        }
    }

    private fun startStream() {
        val movie = MovieCache.get(currentImdbId)
        if (movie != null) {
            currentContentType = LibraryContentType.MOVIE
            val torrent = movie.torrents[currentQuality]
                ?: movie.torrents.values.maxByOrNull { it.seeds }
                ?: run {
                    torrentEngine.setError("No torrent found for quality: $currentQuality")
                    return
                }
            context.startForegroundService(Intent(context, TorrentService::class.java))
            torrentEngine.startStream(torrent)
            resumePositionAfterReady()
            return
        }

        val show = ShowCache.get(currentImdbId)
        if (show == null) {
            torrentEngine.setError("Content not found in cache")
            return
        }
        currentContentType = if (contentTypeStr == "anime") LibraryContentType.ANIME else LibraryContentType.SHOW
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
        torrentEngine.startStream(torrent)
        resumePositionAfterReady()
    }

    private fun resumePositionAfterReady() {
        val key = positionKey()   // capture BEFORE any mutation
        resumeJob?.cancel()
        resumeJob = viewModelScope.launch {
            torrentEngine.state.first { it is StreamState.Ready }.let {
                val savedPos = playbackPositionStore.getPosition(key)
                if (savedPos > 30_000L) {
                    playbackController.command.tryEmit(PlaybackCommand.SeekTo(savedPos))
                }
                startPositionSaveLoop()
            }
            resumeJob = null
        }
    }

    private fun startPositionSaveLoop() {
        val key = positionKey()   // capture ONCE at loop start
        positionSaveJob?.cancel()
        positionSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                playbackPositionStore.savePosition(key, playbackController.playerPositionMs.value)
            }
        }
    }

    fun positionKey(): String {
        val s = currentSeason
        val e = currentEpisode
        return if (s != null && s > 0) "pos_${currentImdbId}_s${s}e${e}" else "pos_${currentImdbId}"
    }

    fun playLocalFile(uriString: String) {
        _uiState.update { it.copy(streamUrl = uriString) }
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

    fun loadCustomSubtitle(uri: android.net.Uri) {
        _uiState.update {
            it.copy(
                subtitleUrl = uri.toString(),
                selectedSubtitle = Subtitle(
                    language = "Custom",
                    fileName = "custom.srt",
                    downloadUrl = uri.toString(),
                    fileId = 0,
                    label = "Custom",
                ),
                showSubtitlePicker = false,
            )
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
        positionSaveJob?.cancel()
        resumeJob?.cancel()
        resumeJob = null
        countdownJob?.cancel()
        countdownJob = null
        pendingNextItem = null
        torrentEngine.stopCurrent()
        context.stopService(Intent(context, TorrentService::class.java))
        castManager.disconnect()
        castManager.dlnaDiscovery.stopDiscovery()
    }

    fun cycleResizeMode() {
        val modes = listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        )
        val currentIndex = modes.indexOf(_resizeMode.value)
        _resizeMode.value = modes[(currentIndex + 1) % modes.size]
    }

    fun setBrightness(v: Float) {
        _brightness.value = v
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownSeconds.value = null
        pendingNextItem = null
    }

    fun onPlaybackCompleted() {
        val completedImdbId = currentImdbId
        val completedSeason = currentSeason
        val completedEpisode = currentEpisode
        val completedQuality = currentQuality
        val completedContentType = currentContentType

        positionSaveJob?.cancel()
        viewModelScope.launch {
            val completedKey = if ((completedSeason ?: 0) > 0)
                "pos_${completedImdbId}_s${completedSeason}e${completedEpisode}"
            else "pos_${completedImdbId}"
            playbackPositionStore.clearPosition(completedKey)
        }

        viewModelScope.launch {
            val metadata = buildLibraryMetadata(completedImdbId, completedSeason, completedEpisode, completedQuality)
            if (metadata != null) libraryRepository.markWatched(completedImdbId, metadata)
        }

        // Trakt scrobble stop
        if (completedImdbId.isNotBlank()) {
            viewModelScope.launch {
                runCatching { traktScrobbleService.scrobbleStop(completedImdbId, completedContentType, 100f) }
            }
        }

        // Auto-play with countdown
        val next = playbackQueue.peek()
        if (next != null) {
            pendingNextItem = next
            _countdownSeconds.value = 10
            countdownJob = viewModelScope.launch {
                for (i in 9 downTo 0) {
                    delay(1_000)
                    _countdownSeconds.value = i
                }
                countdownJob = null
                _countdownSeconds.value = null
                val item = pendingNextItem ?: return@launch
                pendingNextItem = null
                playbackQueue.dequeue()
                advanceToItem(item)
            }
        }
    }

    private fun advanceToItem(next: com.popcorntime.android.data.remote.QueueItem) {
        if (next.magnet.isBlank()) {
            _uiState.update { it.copy(error = "Queue item has no playable URL") }
            return
        }
        currentImdbId = next.imdbId
        currentQuality = next.quality
        currentSeason = next.season
        currentEpisode = next.episode
        currentContentType = next.contentType
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
        resumePositionAfterReady()
    }

    private fun currentProgress(): Float {
        val dur = playbackController.playerDurationMs.value
        val pos = playbackController.playerPositionMs.value
        return if (dur > 0) (pos.toFloat() / dur * 100f) else 0f
    }

    private fun buildLibraryMetadata(
        imdbId: String = currentImdbId,
        @Suppress("UNUSED_PARAMETER") season: Int? = currentSeason,
        @Suppress("UNUSED_PARAMETER") episode: Int? = currentEpisode,
        @Suppress("UNUSED_PARAMETER") quality: String = currentQuality,
    ): LibraryItem? {
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
                contentType = currentContentType,
                addedAt = System.currentTimeMillis(),
            )
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        positionSaveJob?.cancel()
        resumeJob?.cancel()
        countdownJob?.cancel()
        castManager.chromeCaster.unregisterSessionListener()
        castManager.dlnaDiscovery.stopDiscovery()
        castManager.disconnect()
    }
}
