package com.popcorntime.android.data.trakt

import com.popcorntime.android.data.api.dto.TraktAuthState
import com.popcorntime.android.data.api.dto.TraktDeviceCodeResponse
import com.popcorntime.android.data.api.dto.TraktTokenRequest
import com.popcorntime.android.data.api.dto.TraktTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
@Serializable
private data class DeviceCodeRequest(val client_id: String)

class TraktAuthService constructor(
    private val client: HttpClient,
    private val clientId: String,
    private val clientSecret: String,
) {
    suspend fun requestDeviceCode(): TraktDeviceCodeResponse {
        return client.post("oauth/device/code") {
            contentType(ContentType.Application.Json)
            setBody(DeviceCodeRequest(clientId))
        }.body()
    }

    fun pollForToken(deviceCode: String, interval: Int, expiresIn: Int): Flow<TraktAuthState> = flow {
        var pollInterval = interval.coerceAtLeast(5).toLong() * 1000L
        val deadline = System.currentTimeMillis() + expiresIn * 1000L
        emit(TraktAuthState.Pending)
        while (System.currentTimeMillis() < deadline) {
            delay(pollInterval)
            try {
                val response: HttpResponse = client.post("oauth/device/token") {
                    contentType(ContentType.Application.Json)
                    setBody(TraktTokenRequest(code = deviceCode, clientId = clientId, clientSecret = clientSecret))
                }
                when (response.status) {
                    HttpStatusCode.OK -> {
                        emit(TraktAuthState.Authorized(response.body<TraktTokenResponse>()))
                        return@flow
                    }
                    HttpStatusCode.BadRequest -> { /* code not yet authorized, keep polling */ }
                    HttpStatusCode.Gone -> { emit(TraktAuthState.Expired); return@flow }
                    HttpStatusCode.TooManyRequests -> {
                        // Back off: double the poll interval (cap at 60s)
                        pollInterval = minOf(pollInterval * 2, 60_000L)
                    }
                    else -> { emit(TraktAuthState.Error("HTTP ${response.status.value}")); return@flow }
                }
            } catch (e: Exception) {
                emit(TraktAuthState.Error(e.message ?: "Unknown error"))
                return@flow
            }
        }
        emit(TraktAuthState.Expired)
    }
}
