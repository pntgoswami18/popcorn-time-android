package com.popcorntime.android.data.cast

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class KodiCaster(private val httpClient: HttpClient) {

    suspend fun stop(host: String, port: Int): Result<Unit> {
        return runCatching {
            val body = """{"jsonrpc":"2.0","method":"Player.Stop","params":{"playerid":1},"id":1}"""
            val response = httpClient.post("http://$host:$port/jsonrpc") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val text = response.bodyAsText()
            Timber.d("Kodi stop response: $text")
        }
    }

    suspend fun openUrl(host: String, port: Int, streamUrl: String): Result<Unit> {
        return runCatching {
            val encodedUrl = Json.encodeToString(streamUrl)  // produces a JSON string with proper escaping
            val body = """{"jsonrpc":"2.0","method":"Player.Open","params":{"item":{"file":$encodedUrl}},"id":1}"""
            val response = httpClient.post("http://$host:$port/jsonrpc") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val text = response.bodyAsText()
            Timber.d("Kodi response: $text")
        }
    }
}
