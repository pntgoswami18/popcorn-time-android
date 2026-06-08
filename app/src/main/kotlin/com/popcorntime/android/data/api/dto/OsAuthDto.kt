package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OsLoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class OsLoginResponse(
    val token: String = "",
    @SerialName("base_url") val baseUrl: String = "",
    val user: OsUserDto = OsUserDto(),
)

@Serializable
data class OsUserDto(
    @SerialName("allowed_downloads") val allowedDownloads: Int = 0,
    val level: String = "",
)
