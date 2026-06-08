package com.popcorntime.android.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController @Inject constructor() {
    val command = MutableSharedFlow<PlaybackCommand>(extraBufferCapacity = 8)

    private val _playerPositionMs = MutableStateFlow(0L)
    val playerPositionMs: StateFlow<Long> = _playerPositionMs.asStateFlow()

    private val _playerDurationMs = MutableStateFlow(0L)
    val playerDurationMs: StateFlow<Long> = _playerDurationMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun updatePosition(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        _playerPositionMs.value = positionMs
        _playerDurationMs.value = durationMs
        _isPlaying.value = isPlaying
    }

    suspend fun sendCommand(cmd: PlaybackCommand) = command.emit(cmd)
}
