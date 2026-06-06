package com.popcorntime.android.data.trakt

import com.popcorntime.android.data.api.dto.TraktCustomList
import com.popcorntime.android.data.api.dto.TraktCustomListResponse
import com.popcorntime.android.data.api.dto.TraktHistoryEntry
import com.popcorntime.android.data.api.dto.TraktIds
import com.popcorntime.android.data.api.dto.TraktSyncAddRequest
import com.popcorntime.android.data.api.dto.TraktSyncMovie
import com.popcorntime.android.data.api.dto.TraktSyncShow
import com.popcorntime.android.data.api.dto.TraktWatchlistEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Named

class TraktSyncService constructor(
    @Named("trakt") private val client: HttpClient,
    private val tokenStore: TraktTokenStore,
) {
    private suspend fun token(): String? = tokenStore.getAccessToken()

    // ── Watch history ─────────────────────────────────────────────────────────

    suspend fun pullWatchHistory(): List<String> {
        val tk = token() ?: return emptyList()
        val movies = runCatching {
            client.get("sync/history/movies?limit=1000") { bearerAuth(tk) }
                .body<List<TraktHistoryEntry>>()
                .mapNotNull { it.movie?.ids?.imdb }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
        val shows = runCatching {
            client.get("sync/history/shows?limit=1000") { bearerAuth(tk) }
                .body<List<TraktHistoryEntry>>()
                .mapNotNull { it.show?.ids?.imdb }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
        return (movies + shows).distinct()
    }

    suspend fun pushWatched(imdbId: String, watchedAt: Long) {
        val tk = token() ?: return
        val iso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(watchedAt))
        runCatching {
            client.post("sync/history") {
                bearerAuth(tk)
                contentType(ContentType.Application.Json)
                setBody(TraktSyncAddRequest(movies = listOf(TraktSyncMovie(watchedAt = iso, ids = TraktIds(imdb = imdbId)))))
            }
        }
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    suspend fun pullWatchlist(): List<String> {
        val tk = token() ?: return emptyList()
        return runCatching {
            client.get("sync/watchlist") { bearerAuth(tk) }
                .body<List<TraktWatchlistEntry>>()
                .mapNotNull { entry ->
                    when (entry.type) {
                        "movie" -> entry.movie?.ids?.imdb
                        "show" -> entry.show?.ids?.imdb
                        else -> null
                    }
                }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    suspend fun pushToWatchlist(imdbId: String) {
        val tk = token() ?: return
        runCatching {
            client.post("sync/watchlist") {
                bearerAuth(tk)
                contentType(ContentType.Application.Json)
                setBody(TraktSyncAddRequest(movies = listOf(TraktSyncMovie(ids = TraktIds(imdb = imdbId)))))
            }
        }
    }

    suspend fun removeFromWatchlist(imdbId: String) {
        val tk = token() ?: return
        runCatching {
            client.post("sync/watchlist/remove") {
                bearerAuth(tk)
                contentType(ContentType.Application.Json)
                setBody(TraktSyncAddRequest(movies = listOf(TraktSyncMovie(ids = TraktIds(imdb = imdbId)))))
            }
        }
    }

    // ── Favourites (Trakt custom list) ────────────────────────────────────────

    private suspend fun ensureFavouritesSlug(): String? {
        val tk = token() ?: return null
        val existing = tokenStore.getFavouritesSlug()
        if (existing != null) return existing
        return runCatching {
            val resp = client.post("users/me/lists") {
                bearerAuth(tk)
                contentType(ContentType.Application.Json)
                setBody(TraktCustomList(name = "PopcornTime Favourites", privacy = "private"))
            }.body<TraktCustomListResponse>()
            tokenStore.saveFavouritesSlug(resp.ids.slug)
            resp.ids.slug
        }.getOrNull()
    }

    suspend fun pullFavourites(): List<String> {
        val tk = token() ?: return emptyList()
        val slug = ensureFavouritesSlug() ?: return emptyList()
        return runCatching {
            client.get("users/me/lists/$slug/items") { bearerAuth(tk) }
                .body<List<TraktWatchlistEntry>>()
                .mapNotNull { entry ->
                    when (entry.type) {
                        "movie" -> entry.movie?.ids?.imdb
                        "show" -> entry.show?.ids?.imdb
                        else -> null
                    }
                }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    suspend fun pushFavourite(imdbId: String) {
        val tk = token() ?: return
        val slug = ensureFavouritesSlug() ?: return
        runCatching {
            client.post("users/me/lists/$slug/items") {
                bearerAuth(tk)
                contentType(ContentType.Application.Json)
                setBody(TraktSyncAddRequest(movies = listOf(TraktSyncMovie(ids = TraktIds(imdb = imdbId)))))
            }
        }
    }

    suspend fun removeFavourite(imdbId: String) {
        val tk = token() ?: return
        val slug = ensureFavouritesSlug() ?: return
        runCatching {
            client.post("users/me/lists/$slug/items/remove") {
                bearerAuth(tk)
                contentType(ContentType.Application.Json)
                setBody(TraktSyncAddRequest(movies = listOf(TraktSyncMovie(ids = TraktIds(imdb = imdbId)))))
            }
        }
    }
}
