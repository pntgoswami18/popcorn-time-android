package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.YtsListResponse
import com.popcorntime.android.data.api.dto.YtsMovieDetailResponse
import com.popcorntime.android.data.api.dto.YtsMovieDto
import com.popcorntime.android.domain.model.MovieFilter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MovieApiService @Inject constructor(
    private val client: HttpClient,
    @Named("movieServers") private val servers: List<String>,
) {
    // Rotate through servers on failure — mirrors butter-provider/generic.js setApiUrls + shuffle
    private val serverQueue = ArrayDeque(servers.shuffled())
    private val serverMutex = Mutex()

    suspend fun listMovies(filter: MovieFilter): YtsListResponse {
        return serverMutex.withLock {
            val errors = mutableListOf<Throwable>()
            repeat(serverQueue.size) {
                val base = serverQueue.first()
                try {
                    val response = client.get("${base}api/v2/list_movies.json") {
                        parameter("page", filter.page)
                        parameter("limit", 20)
                        parameter("sort_by", filter.sortBy)
                        parameter("order_by", filter.orderBy)
                        parameter("with_rt_ratings", true)
                        if (filter.genre != "All") parameter("genre", filter.genre)
                        if (filter.quality != "All") parameter("quality", filter.quality)
                        if (filter.minimumRating > 0) parameter("minimum_rating", filter.minimumRating)
                        if (filter.queryTerm.isNotBlank()) parameter("query_term", filter.queryTerm)
                    }.body<YtsListResponse>()
                    // On success, promote this server to head
                    serverQueue.remove(base)
                    serverQueue.addFirst(base)
                    return@withLock response
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
    }

    suspend fun getMovieByImdbId(imdbId: String): YtsMovieDto {
        return serverMutex.withLock {
            val errors = mutableListOf<Throwable>()
            repeat(serverQueue.size) {
                val base = serverQueue.first()
                try {
                    val response = client.get("${base}api/v2/movie_details.json") {
                        parameter("imdb_id", imdbId)
                    }.body<YtsMovieDetailResponse>()
                    serverQueue.remove(base)
                    serverQueue.addFirst(base)
                    return@withLock response.data.movie
                } catch (e: Exception) {
                    Timber.w(e, "Server $base failed for IMDB lookup, rotating")
                    errors += e
                    serverQueue.remove(base)
                    serverQueue.addLast(base)
                }
            }
            throw errors.lastOrNull() ?: IllegalStateException("No servers configured")
        }
    }
}
