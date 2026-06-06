package com.popcorntime.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraktDeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_url") val verificationUrl: String,
    @SerialName("expires_in") val expiresIn: Int,
    val interval: Int,
)

@Serializable
data class TraktTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class TraktTokenRequest(
    val code: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String = "",
    @SerialName("grant_type") val grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
)

@Serializable
data class TraktIds(
    val trakt: Int = 0,
    val slug: String = "",
    val imdb: String = "",
    val tmdb: Int = 0,
    val tvdb: Int = 0,
)

@Serializable
data class TraktMovieItem(
    val title: String = "",
    val year: Int = 0,
    val ids: TraktIds = TraktIds(),
)

@Serializable
data class TraktShowItem(
    val title: String = "",
    val year: Int = 0,
    val ids: TraktIds = TraktIds(),
)

@Serializable
data class TraktHistoryEntry(
    val id: Long = 0,
    @SerialName("watched_at") val watchedAt: String = "",
    val action: String = "",
    val type: String = "",
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null,
)

@Serializable
data class TraktWatchlistEntry(
    val id: Long = 0,
    @SerialName("listed_at") val listedAt: String = "",
    val type: String = "",
    val movie: TraktMovieItem? = null,
    val show: TraktShowItem? = null,
)

@Serializable
data class TraktSyncMovie(
    @SerialName("watched_at") val watchedAt: String? = null,
    val ids: TraktIds,
)

@Serializable
data class TraktSyncShow(
    val ids: TraktIds,
)

@Serializable
data class TraktSyncAddRequest(
    val movies: List<TraktSyncMovie>? = null,
    val shows: List<TraktSyncShow>? = null,
)

@Serializable
data class TraktCustomList(
    val name: String,
    val description: String = "",
    val privacy: String = "private",
    @SerialName("allow_comments") val allowComments: Boolean = false,
    @SerialName("display_numbers") val displayNumbers: Boolean = false,
)

@Serializable
data class TraktCustomListResponse(
    val name: String = "",
    val ids: TraktListIds = TraktListIds(),
)

@Serializable
data class TraktListIds(
    val trakt: Int = 0,
    val slug: String = "",
)

sealed class TraktAuthState {
    object Pending : TraktAuthState()
    data class Authorized(val token: TraktTokenResponse) : TraktAuthState()
    object Expired : TraktAuthState()
    data class Error(val message: String) : TraktAuthState()
}
