package com.popcorntime.android.data.trakt

import com.popcorntime.android.domain.model.LibraryContentType
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TraktScrobbleService @Inject constructor(
    @Named("trakt") private val client: HttpClient,
    private val tokenStore: TraktTokenStore,
) {
    private suspend fun scrobble(
        action: String,
        imdbId: String,
        contentType: LibraryContentType,
        progress: Float,
    ) {
        val token = tokenStore.getAccessToken() ?: return
        val body = when (contentType) {
            LibraryContentType.MOVIE ->
                """{"movie":{"ids":{"imdb":"$imdbId"}},"progress":$progress}"""
            else ->
                """{"show":{"ids":{"imdb":"$imdbId"}},"progress":$progress}"""
        }
        runCatching {
            client.post("https://api.trakt.tv/scrobble/$action") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun scrobbleStart(imdbId: String, contentType: LibraryContentType, progress: Float) =
        scrobble("start", imdbId, contentType, progress)

    suspend fun scrobblePause(imdbId: String, contentType: LibraryContentType, progress: Float) =
        scrobble("pause", imdbId, contentType, progress)

    suspend fun scrobbleStop(imdbId: String, contentType: LibraryContentType, progress: Float) =
        scrobble("stop", imdbId, contentType, progress)
}
