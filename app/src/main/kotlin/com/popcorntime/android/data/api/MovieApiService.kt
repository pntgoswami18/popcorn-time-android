package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.ButterMovieDto
import com.popcorntime.android.domain.model.MovieFilter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
    private val rotation = ServerRotation(servers)

    suspend fun listMovies(filter: MovieFilter): List<ButterMovieDto> = rotation.withRotation { base ->
        client.get("${base}movies/${filter.page}") {
            expectSuccess = true
            parameter("sort", toButterSort(filter.sortBy))
            parameter("order", if (filter.orderBy.equals("asc", ignoreCase = true)) 1 else -1)
            if (filter.genre != "All Genre") parameter("genre", filter.genre.lowercase())
            if (filter.queryTerm.isNotBlank()) parameter("keywords", filter.queryTerm)
        }.body()
    }

    suspend fun getMovieByImdbId(imdbId: String): ButterMovieDto = rotation.withRotation { base ->
        client.get("${base}movie/$imdbId") {
            expectSuccess = true
        }.body()
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
