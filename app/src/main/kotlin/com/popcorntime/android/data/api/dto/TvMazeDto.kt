package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── TVMaze REST API DTOs ──────────────────────────────────────────────────────
// Docs: https://www.tvmaze.com/api

@Serializable
data class TvMazeShowDto(
    val id: Int = 0,
    val name: String = "",
    /** "Scripted", "Animation", "Reality", "Talk Show", "Documentary", etc. */
    val type: String = "",
    val genres: List<String> = emptyList(),
    val status: String = "",                 // "Running", "Ended", "In Development"
    val premiered: String? = null,           // "2011-04-17"
    val ended: String? = null,
    val rating: TvMazeRatingDto = TvMazeRatingDto(),
    val image: TvMazeImageDto? = null,
    val summary: String? = null,             // HTML string
    val network: TvMazeNetworkDto? = null,
    @SerialName("webChannel") val webChannel: TvMazeNetworkDto? = null,
    val runtime: Int? = null,
    @SerialName("averageRuntime") val averageRuntime: Int? = null,
    @SerialName("_embedded") val embedded: TvMazeEmbeddedDto? = null,
    val externals: TvMazeExternalsDto? = null,
)

@Serializable
data class TvMazeRatingDto(val average: Double? = null)

@Serializable
data class TvMazeImageDto(
    val medium: String? = null,
    val original: String? = null,
)

@Serializable
data class TvMazeNetworkDto(
    val id: Int = 0,
    val name: String = "",
    val country: TvMazeCountryDto? = null,
)

@Serializable
data class TvMazeCountryDto(
    val name: String = "",
    val code: String = "",
)

@Serializable
data class TvMazeEmbeddedDto(
    val episodes: List<TvMazeEpisodeDto> = emptyList(),
)

@Serializable
data class TvMazeEpisodeDto(
    val id: Int = 0,
    val name: String = "",
    val season: Int = 0,
    val number: Int? = null,   // null for specials
    val airdate: String = "",  // "2011-04-17"
    val runtime: Int? = null,
    val image: TvMazeImageDto? = null,
    val summary: String? = null,
)

@Serializable
data class TvMazeExternalsDto(
    val imdb: String? = null,     // "tt0944947"
    val thetvdb: Int? = null,
)

/** Wrapper returned by GET /search/shows?q={query} */
@Serializable
data class TvMazeSearchResultDto(
    val score: Double = 0.0,
    val show: TvMazeShowDto,
)
