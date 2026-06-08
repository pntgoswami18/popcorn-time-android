package com.popcorntime.android.data.subtitles

import com.popcorntime.android.data.api.dto.OsLoginRequest
import com.popcorntime.android.data.api.dto.OsLoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import timber.log.Timber

sealed class OsLoginResult {
    data class Success(
        val token: String,
        val baseUrl: String,
        val allowedDownloads: Int,
    ) : OsLoginResult()

    data class Failure(val reason: String) : OsLoginResult()
}

class OsAuthService constructor(
    private val client: HttpClient,
    private val osTokenStore: OsTokenStore,
) {
    companion object {
        private const val BASE_URL = "https://api.opensubtitles.com/api/v1"
    }

    suspend fun login(username: String, password: String): OsLoginResult {
        return try {
            val response = client.post("$BASE_URL/login") {
                header("Api-Key", osTokenStore.resolveApiKey())
                contentType(ContentType.Application.Json)
                setBody(OsLoginRequest(username = username, password = password))
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    val body = response.body<OsLoginResponse>()
                    if (body.token.isBlank()) {
                        OsLoginResult.Failure("Empty token in response")
                    } else {
                        OsLoginResult.Success(
                            token = body.token,
                            baseUrl = body.baseUrl,
                            allowedDownloads = body.user.allowedDownloads,
                        )
                    }
                }
                HttpStatusCode.Unauthorized -> OsLoginResult.Failure("Invalid username or password")
                else -> OsLoginResult.Failure("HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            Timber.w(e, "OsAuthService: login failed")
            OsLoginResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun logout(token: String): Boolean {
        return try {
            val response = client.delete("$BASE_URL/logout") {
                header("Api-Key", osTokenStore.resolveApiKey())
                header("Authorization", "Bearer $token")
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent
        } catch (e: Exception) {
            Timber.w(e, "OsAuthService: logout failed")
            false
        }
    }
}
