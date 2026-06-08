package com.popcorntime.android.domain.model

data class Movie(
    val id: Int,
    val imdbId: String,
    val title: String,
    val year: Int,
    val rating: Double,
    val runtime: Int,          // minutes
    val genres: List<String>,
    val synopsis: String,
    val posterUrl: String,
    val coverUrl: String,
    val backdropUrl: String,
    val trailerUrl: String?,
    val certification: String,
    val language: String,
    val torrents: Map<String, Torrent>,  // quality -> torrent
    val isWatched: Boolean = false,
    val isBookmarked: Boolean = false,
)

data class Torrent(
    val url: String,
    val magnet: String,
    val quality: String,
    val type: String,           // "bluray", "web", etc.
    val size: Long,             // bytes
    val fileSize: String,       // human-readable e.g. "1.5 GB"
    val seeds: Int,
    val peers: Int,
    val hash: String,
    val source: TorrentSource = TorrentSource.YTS,
)
