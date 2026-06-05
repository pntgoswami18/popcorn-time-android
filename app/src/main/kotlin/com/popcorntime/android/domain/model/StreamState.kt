package com.popcorntime.android.domain.model

sealed class StreamState {
    data object Idle : StreamState()
    data class Buffering(
        val progress: Float,   // 0..1
        val downloadSpeed: Long,
        val uploadSpeed: Long,
        val seeds: Int,
        val peers: Int,
    ) : StreamState()
    data class Ready(
        val streamUrl: String,
        val downloadSpeed: Long,
        val uploadSpeed: Long,
        val seeds: Int,
        val peers: Int,
        val progress: Float,
    ) : StreamState()
    data class Error(val message: String) : StreamState()
}

data class StreamInfo(
    val movie: Movie,
    val torrent: Torrent,
    val quality: String,
    val subtitleLanguage: String = "none",
)
