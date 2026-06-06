package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YtsListResponse(
    val status: String,
    @SerialName("status_message") val statusMessage: String,
    val data: YtsMovieListData,
)

@Serializable
data class YtsMovieListData(
    @SerialName("movie_count") val movieCount: Int = 0,
    val limit: Int = 20,
    @SerialName("page_number") val pageNumber: Int = 1,
    val movies: List<YtsMovieDto>? = null,
)

/** Response wrapper for the /api/v2/movie_details.json endpoint (returns a single movie). */
@Serializable
data class YtsMovieDetailResponse(
    val status: String,
    @SerialName("status_message") val statusMessage: String,
    val data: YtsMovieDetailData,
)

@Serializable
data class YtsMovieDetailData(
    val movie: YtsMovieDto,
)

@Serializable
data class YtsMovieDto(
    val id: Int,
    val url: String = "",
    @SerialName("imdb_code") val imdbCode: String = "",
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String = "",
    val slug: String = "",
    val year: Int = 0,
    val rating: Double = 0.0,
    val runtime: Int = 0,
    val genres: List<String>? = null,
    val summary: String = "",
    @SerialName("description_full") val descriptionFull: String = "",
    val synopsis: String = "",
    @SerialName("yt_trailer_code") val ytTrailerCode: String = "",
    val language: String = "en",
    @SerialName("mpa_rating") val mpaRating: String = "",
    @SerialName("background_image") val backgroundImage: String = "",
    @SerialName("background_image_original") val backgroundImageOriginal: String = "",
    @SerialName("small_cover_image") val smallCoverImage: String = "",
    @SerialName("medium_cover_image") val mediumCoverImage: String = "",
    @SerialName("large_cover_image") val largeCoverImage: String = "",
    val torrents: List<YtsTorrentDto>? = null,
)

@Serializable
data class YtsTorrentDto(
    val url: String = "",
    val hash: String = "",
    val quality: String = "",
    val type: String = "",
    val seeds: Int = 0,
    val peers: Int = 0,
    val size: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("date_uploaded") val dateUploaded: String = "",
)
