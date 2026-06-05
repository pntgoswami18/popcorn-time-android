package com.popcorntime.android.ui.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.torrent.TorrentEngine
import com.popcorntime.android.data.torrent.TorrentService
import com.popcorntime.android.domain.model.StreamState
import com.popcorntime.android.ui.movies.MovieCache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val torrentEngine: TorrentEngine,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val imdbId: String = checkNotNull(savedStateHandle["imdbId"])
    private val quality: String = checkNotNull(savedStateHandle["quality"])

    val streamState: StateFlow<StreamState> = torrentEngine.state

    init { startStream() }

    private fun startStream() {
        val movie = MovieCache.get(imdbId) ?: return
        val torrent = movie.torrents[quality]
            ?: movie.torrents.values.maxByOrNull { it.seeds }
            ?: return

        // Start foreground service so streaming survives backgrounding
        context.startForegroundService(Intent(context, TorrentService::class.java))

        viewModelScope.launch {
            torrentEngine.startStream(torrent)
        }
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
