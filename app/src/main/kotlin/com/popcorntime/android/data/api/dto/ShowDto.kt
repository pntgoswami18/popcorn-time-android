package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowDto(
    @SerialName("_id") val id: String = "",
    @SerialName("imdb_id") val imdbId: String = "",
    @SerialName("tvdb_id") val tvdbId: String = "",
    val title: String = "",
    val year: String = "",
    val slug: String = "",
    val synopsis: String = "",
    val runtime: String = "",
    val country: String = "",
    val network: String = "",
    @SerialName("air_day") val airDay: String = "",
    @SerialName("air_time") val airTime: String = "",
    val status: String = "",
    @SerialName("num_seasons") val numSeasons: Int = 0,
    val rating: ShowRatingDto = ShowRatingDto(),
    val genres: List<String> = emptyList(),
    val images: ShowImagesDto = ShowImagesDto(),
    val episodes: List<EpisodeDto> = emptyList(),
)

@Serializable
data class ShowRatingDto(
    val percentage: Int = 0,
    val watching: Int = 0,
    val votes: Int = 0,
    val loved: Int = 0,
    val hated: Int = 0,
)

@Serializable
data class ShowImagesDto(
    val poster: String = "",
    val fanart: String = "",
    val banner: String = "",
)

@Serializable
data class EpisodeDto(
    @SerialName("tvdb_id") val tvdbId: Int = 0,
    val season: Int = 0,
    val episode: Int = 0,
    val title: String = "",
    val overview: String = "",
    @SerialName("date_based") val dateBased: Boolean = false,
    @SerialName("first_aired") val firstAired: Long = 0,
    val torrents: Map<String, EpisodeTorrentDto> = emptyMap(),
)

@Serializable
data class EpisodeTorrentDto(
    val provider: String = "",
    val peers: Int = 0,
    val seeds: Int = 0,
    val url: String = "",
)
