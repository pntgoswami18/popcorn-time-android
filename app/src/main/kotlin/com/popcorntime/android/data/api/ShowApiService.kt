package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.ShowDto
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.ShowFilter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ShowApiService @Inject constructor(
    private val client: HttpClient,
    @Named("showServers") private val serverQueue: ArrayDeque<String>,
) {

    private val contentPath = { type: ContentType ->
        if (type == ContentType.SHOW) "shows" else "animes"
    }

    private val detailPath = { type: ContentType ->
        if (type == ContentType.SHOW) "show" else "anime"
    }

    /** List shows/anime — page-based, mirrors butter-provider/tv.js fetch() */
    suspend fun listShows(filter: ShowFilter): List<ShowDto> {
        val path = contentPath(filter.type)
        val errors = mutableListOf<Throwable>()
        repeat(serverQueue.size) {
            val base = serverQueue.first()
            try {
                val result = client.get("$base$path/${filter.page}") {
                    parameter("sort", filter.sortBy)
                    parameter("order", filter.order)
                    if (filter.genre != "All") parameter("genre", filter.genre)
                    if (filter.keywords.isNotBlank()) parameter("keywords", filter.keywords)
                }.body<List<ShowDto>>()
                serverQueue.remove(base); serverQueue.addFirst(base)
                return result
            } catch (e: Exception) {
                Timber.w(e, "ShowApiService: server $base failed, rotating")
                errors += e
                serverQueue.remove(base); serverQueue.addLast(base)
            }
        }
        throw errors.last()
    }

    /** Full show detail including all episodes and torrents */
    suspend fun getShowDetail(imdbId: String, type: ContentType = ContentType.SHOW): ShowDto {
        val path = detailPath(type)
        val errors = mutableListOf<Throwable>()
        repeat(serverQueue.size) {
            val base = serverQueue.first()
            try {
                val result = client.get("$base$path/$imdbId").body<ShowDto>()
                serverQueue.remove(base); serverQueue.addFirst(base)
                return result
            } catch (e: Exception) {
                errors += e
                serverQueue.remove(base); serverQueue.addLast(base)
            }
        }
        throw errors.last()
    }
}
