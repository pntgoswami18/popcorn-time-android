package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the Butter / popcorn-ru API (https://github.com/popcorn-time-ru/popcorn-ru)
 * served by the popcorn-desktop mirror domains (fusme.link, jfper.link, ...).
 *
 * Endpoints:
 *  - GET {base}movies/{page}?sort=<sort>&order=<-1|1>[&genre=..][&keywords=..]  → JSON array of [ButterMovieDto]
 *  - GET {base}movie/{imdbId}                                                   → single [ButterMovieDto]
 *
 * Shape verified live (2026-06) against https://fusme.link/. Notes from real responses:
 *  - `year`, `runtime` and torrent `size` are STRINGS, `released` is a unix timestamp (Int).
 *  - `torrents` is keyed by language ("en", ...) then by quality ("720p"/"1080p"/"2160p").
 *  - torrent `url` is a magnet link with HTML-escaped ampersands (`&amp;`).
 *  - `rating.percentage` is 0..100.
 */
@Serializable
data class ButterMovieDto(
    @SerialName("_id") val id: String,
    @SerialName("imdb_id") val imdbId: String = "",
    @SerialName("tmdb_id") val tmdbId: Int? = null,
    val title: String = "",
    val year: String? = null,
    @SerialName("original_language") val originalLanguage: String = "",
    val synopsis: String = "",
    val runtime: String? = null,
    val released: Long? = null,
    val certification: String = "",
    val trailer: String? = null,
    val genres: List<String>? = null,
    val images: ButterImagesDto? = null,
    val rating: ButterRatingDto? = null,
    /** language -> quality -> torrent */
    val torrents: Map<String, Map<String, ButterTorrentDto>>? = null,
)

@Serializable
data class ButterImagesDto(
    val poster: String? = null,
    val fanart: String? = null,
    val banner: String? = null,
)

@Serializable
data class ButterRatingDto(
    val percentage: Double = 0.0,
    val watching: Int = 0,
    val votes: Int = 0,
    val loved: Int = 0,
    val hated: Int = 0,
)

@Serializable
data class ButterTorrentDto(
    /** Magnet link. May contain HTML-escaped `&amp;` separators. */
    val url: String = "",
    val provider: String = "",
    val source: String? = null,
    val title: String? = null,
    val quality: String? = null,
    val seed: Int = 0,
    val peer: Int = 0,
    /** Size in bytes, as a string (e.g. "6764573491"). */
    val size: String? = null,
    /** Human-readable size (e.g. "6.3 GB"). */
    val filesize: String? = null,
)
