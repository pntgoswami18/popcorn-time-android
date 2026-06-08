package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── EZTV REST API DTOs ────────────────────────────────────────────────────────
// Docs: https://eztv.re/api/

@Serializable
data class EztvResponse(
    @SerialName("torrents_count") val torrentsCount: Int = 0,
    val limit: Int = 0,
    val page: Int = 0,
    val torrents: List<EztvTorrentDto> = emptyList(),
)

@Serializable
data class EztvTorrentDto(
    val id: Long = 0,
    val hash: String = "",
    @SerialName("magnet_url") val magnetUrl: String = "",
    val filename: String = "",
    val title: String = "",
    @SerialName("imdb_id") val imdbId: String = "",
    /** Season number as string, e.g. "1" */
    val season: String = "",
    /** Episode number as string, e.g. "3" */
    val episode: String = "",
    val seeds: Int = 0,
    val peers: Int = 0,
    /** File size in bytes, serialised as a string by the API */
    @SerialName("size_bytes") val sizeBytes: String = "0",
)
