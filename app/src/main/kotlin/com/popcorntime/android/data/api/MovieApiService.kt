package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.ButterMovieDto
import com.popcorntime.android.domain.model.MovieFilter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Movie provider speaking the Butter / popcorn-ru API — the same API and the same
 * mirror domains popcorn-desktop uses (fusme.link, jfper.link, uxert.link, ...).
 *
 *  - GET {base}movies/{page}?sort=..&order=-1[&genre=..][&keywords=..] → JSON array (~50/page, [] = end)
 *  - GET {base}movie/{imdbId}                                          → single movie object
 */
@Singleton
class MovieApiService @Inject constructor(
    private val client: HttpClient,
    @Named("movieServers") private val servers: List<String>,
) {
    // Rotate through servers on failure — mirrors butter-provider/generic.js setApiUrls + shuffle
    private val serverQueue = ArrayDeque(servers.shuffled())
    private val serverMutex = Mutex()

    suspend fun listMovies(filter: MovieFilter): List<ButterMovieDto> = withRotation { base ->
        client.get("${base}movies/${filter.page}") {
            expectSuccess = true
            parameter("sort", toButterSort(filter.sortBy))
            parameter("order", if (filter.orderBy.equals("asc", ignoreCase = true)) 1 else -1)
            if (filter.genre != "All") parameter("genre", filter.genre.lowercase())
            if (filter.queryTerm.isNotBlank()) parameter("keywords", filter.queryTerm)
        }.body()
    }

    suspend fun getMovieByImdbId(imdbId: String): ButterMovieDto = withRotation { base ->
        client.get("${base}movie/$imdbId") {
            expectSuccess = true
        }.body()
    }

    /**
     * Runs [block] against the head server; on failure rotates it to the back and
     * retries with the next one. On success the working server is kept at the head.
     */
    private suspend fun <T> withRotation(block: suspend (base: String) -> T): T =
        serverMutex.withLock {
            val errors = mutableListOf<Throwable>()
            repeat(serverQueue.size) {
                val base = serverQueue.first()
                try {
                    val result = block(base)
                    // On success, promote this server to head
                    serverQueue.remove(base)
                    serverQueue.addFirst(base)
                    return@withLock result
                } catch (e: Exception) {
                    Timber.w(e, "Server $base failed, rotating")
                    errors += e
                    // Rotate — move failed server to the back
                    serverQueue.remove(base)
                    serverQueue.addLast(base)
                }
            }
            throw errors.lastOrNull() ?: IllegalStateException("No servers configured")
        }

    companion object {
        /**
         * Maps a sort value to a Butter sort. Valid Butter sorts (verified live):
         * "trending", "popularity", "last added", "year", "title", "rating".
         * Legacy YTS-style values are translated for backward compatibility.
         */
        fun toButterSort(sortBy: String): String = when (sortBy.lowercase()) {
            "date_added", "last added" -> "last added"
            "download_count", "trending" -> "trending"
            "like_count", "popularity" -> "popularity"
            "seeds" -> "trending"
            "year", "title", "rating" -> sortBy.lowercase()
            else -> "trending"
        }
    }
}
