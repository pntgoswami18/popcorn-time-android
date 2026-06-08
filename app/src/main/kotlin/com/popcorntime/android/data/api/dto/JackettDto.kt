package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JackettResultsResponse(
    @SerialName("Results") val results: List<JackettResultDto> = emptyList()
)

@Serializable
data class JackettResultDto(
    @SerialName("Title") val title: String = "",
    @SerialName("MagnetUri") val magnetUri: String = "",
    @SerialName("Link") val link: String = "",
    @SerialName("Size") val size: Long = 0,
    @SerialName("Seeders") val seeders: Int = 0,
    @SerialName("Peers") val peers: Int = 0,
    @SerialName("InfoHash") val infoHash: String = "",
    @SerialName("CategoryDesc") val categoryDesc: String = "",
)
