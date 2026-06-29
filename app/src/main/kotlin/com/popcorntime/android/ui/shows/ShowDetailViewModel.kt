package com.popcorntime.android.ui.shows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.torrent.DownloadManager
import com.popcorntime.android.data.torrent.DownloadStats
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.Episode
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Per-episode download state ────────────────────────────────────────────────

sealed class EpisodeDownloadState {
    object NotDownloaded : EpisodeDownloadState()
    object Queued : EpisodeDownloadState()
    data class Downloading(val stats: DownloadStats) : EpisodeDownloadState()
    data class Downloaded(val localUri: String?) : EpisodeDownloadState()
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class ShowDetailUiState(
    val show: Show? = null,
    val seasons: List<Season> = emptyList(),
    val selectedSeason: Int = 1,
    val isLoading: Boolean = true,
    /** True while the detail/episode fetch is in-flight (even when cached show is already shown). */
    val isEpisodesLoading: Boolean = true,
    val error: String? = null,
    /** Non-null when the episodes fetch failed but cached show data is still displayed. */
    val episodesError: String? = null,
    val isWatched: Boolean = false,
    val isBookmarked: Boolean = false,
    val isInWatchlist: Boolean = false,
    val allWatched: Boolean = false,
    /** Composite keys "imdbId_sXeY" for episodes the user has watched. */
    val watchedEpisodeKeys: Set<String> = emptySet(),
    /** key = "imdbId_sXeY" → download state for that episode. */
    val episodeDownloads: Map<String, EpisodeDownloadState> = emptyMap(),
    /** One-shot message to surface in a snackbar (e.g. concurrent-download rejection). */
    val transientMessage: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ShowDetailViewModel @Inject constructor(
    private val repository: ShowRepository,
    private val libraryRepository: LibraryRepository,
    private val downloadManager: DownloadManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val imdbId: String = checkNotNull(savedStateHandle["imdbId"])
    private val typeStr: String = savedStateHandle["contentType"] ?: "show"
    private val contentType = if (typeStr == "anime") ContentType.ANIME else ContentType.SHOW

    private val _uiState = MutableStateFlow(ShowDetailUiState())
    val uiState: StateFlow<ShowDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
        observeDownloads()
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private fun loadDetail() {
        val cached = ShowCache.get(imdbId)
        if (cached != null) {
            val seasons = cached.seasons()
            _uiState.update {
                it.copy(
                    show = cached,
                    seasons = seasons,
                    selectedSeason = seasons.firstOrNull()?.number ?: 1,
                    isLoading = false,
                    // isEpisodesLoading stays true — full detail still in-flight
                )
            }
        }

        viewModelScope.launch {
            val inWatchlist = libraryRepository.isInWatchlist(imdbId)
            val isFavourited = libraryRepository.isFavourited(imdbId)
            val isWatched = libraryRepository.isWatched(imdbId)
            val watchedIds = libraryRepository.observeWatched().first().map { it.imdbId }.toSet()
            _uiState.update {
                it.copy(
                    isInWatchlist = inWatchlist,
                    isBookmarked = isFavourited,
                    isWatched = isWatched,
                    watchedEpisodeKeys = watchedIds,
                )
            }

            repository.getShowDetail(imdbId, contentType).fold(
                onSuccess = { show ->
                    ShowCache.put(show)
                    val seasons = show.seasons()
                    val epKeys = show.episodes.map { ep -> "${show.imdbId}_s${ep.season}e${ep.episode}" }
                    val allWatched = epKeys.isNotEmpty() && epKeys.all { it in watchedIds }
                    _uiState.update {
                        it.copy(
                            show = show,
                            seasons = seasons,
                            selectedSeason = seasons.firstOrNull()?.number ?: 1,
                            isLoading = false,
                            isEpisodesLoading = false,
                            episodesError = null,
                            allWatched = allWatched,
                        )
                    }
                },
                onFailure = { e ->
                    if (_uiState.value.show == null) {
                        _uiState.update { it.copy(isLoading = false, isEpisodesLoading = false, error = e.message) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isEpisodesLoading = false,
                                episodesError = e.message ?: "Failed to load episodes",
                            )
                        }
                    }
                },
            )
        }
    }

    // ── Download observation ──────────────────────────────────────────────────

    private fun observeDownloads() {
        val episodeKeyPrefix = "${imdbId}_s"
        viewModelScope.launch {
            combine(
                downloadManager.downloads,
                downloadManager.activeDownloadStats,
                downloadManager.activeImdbId,
            ) { downloads, stats, activeId ->
                Triple(downloads, stats, activeId)
            }.collect { (downloads, stats, activeId) ->
                val map = downloads
                    .filter { it.imdbId.startsWith(episodeKeyPrefix) }
                    .associate { entity ->
                        val state: EpisodeDownloadState = when {
                            entity.completedAt != null ->
                                EpisodeDownloadState.Downloaded(entity.filePath)
                            entity.imdbId == activeId && stats != null ->
                                EpisodeDownloadState.Downloading(stats)
                            else -> EpisodeDownloadState.Queued
                        }
                        entity.imdbId to state
                    }
                _uiState.update { it.copy(episodeDownloads = map) }
            }
        }
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun retryEpisodes() {
        _uiState.update { it.copy(isEpisodesLoading = true, episodesError = null) }
        loadDetail()
    }

    fun selectSeason(number: Int) {
        _uiState.update { it.copy(selectedSeason = number) }
    }

    fun startEpisodeDownload(episode: Episode, quality: String) {
        val show = _uiState.value.show ?: return
        val episodeTorrent = episode.torrents[quality] ?: return
        val episodeKey = "${show.imdbId}_s${episode.season}e${episode.episode}"
        val title = "${show.title} S${episode.season.toString().padStart(2, '0')}E${episode.episode.toString().padStart(2, '0')}"
        val started = downloadManager.startDownload(
            imdbId = episodeKey,
            title = title,
            magnetUrl = episodeTorrent.url,
            quality = quality,
        )
        if (!started) {
            _uiState.update { it.copy(transientMessage = "Another download is already in progress") }
        }
    }

    fun cancelEpisodeDownload(episode: Episode) {
        val show = _uiState.value.show ?: return
        val episodeKey = "${show.imdbId}_s${episode.season}e${episode.episode}"
        downloadManager.cancelDownload(episodeKey)
    }

    fun consumeTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    // ── Watch state ───────────────────────────────────────────────────────────

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

    fun toggleEpisodeWatched(episode: Episode) {
        val show = _uiState.value.show ?: return
        val key = "${show.imdbId}_s${episode.season}e${episode.episode}"
        viewModelScope.launch {
            val isCurrentlyWatched = key in _uiState.value.watchedEpisodeKeys
            val item = LibraryItem(
                imdbId = key,
                title = "${show.title} S${episode.season}E${episode.episode}",
                posterUrl = show.posterUrl,
                year = show.year,
                contentType = if (contentType == ContentType.ANIME) LibraryContentType.ANIME else LibraryContentType.SHOW,
                addedAt = System.currentTimeMillis(),
            )
            if (isCurrentlyWatched) {
                libraryRepository.unmarkWatched(key)
                _uiState.update { it.copy(watchedEpisodeKeys = it.watchedEpisodeKeys - key) }
            } else {
                libraryRepository.markWatched(key, item)
                _uiState.update { it.copy(watchedEpisodeKeys = it.watchedEpisodeKeys + key) }
            }
        }
    }

    fun markAllWatched() {
        viewModelScope.launch {
            val show = _uiState.value.show ?: return@launch
            show.episodes.forEach { ep ->
                val epKey = "${show.imdbId}_s${ep.season}e${ep.episode}"
                val epItem = LibraryItem(
                    imdbId = epKey,
                    title = "${show.title} S${ep.season}E${ep.episode}",
                    posterUrl = show.posterUrl,
                    year = show.year,
                    contentType = if (contentType == ContentType.ANIME) LibraryContentType.ANIME else LibraryContentType.SHOW,
                    addedAt = System.currentTimeMillis(),
                )
                libraryRepository.markWatched(epKey, epItem)
            }
            _uiState.update { it.copy(allWatched = true) }
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
