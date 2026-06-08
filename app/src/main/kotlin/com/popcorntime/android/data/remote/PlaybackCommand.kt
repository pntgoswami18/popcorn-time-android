package com.popcorntime.android.data.remote

sealed class PlaybackCommand {
    data object Play : PlaybackCommand()
    data object Pause : PlaybackCommand()
    data class SeekTo(val positionMs: Long) : PlaybackCommand()
    data object Stop : PlaybackCommand()
}
