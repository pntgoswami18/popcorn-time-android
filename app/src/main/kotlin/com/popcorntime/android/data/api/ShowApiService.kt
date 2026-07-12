package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.EztvResponse
import com.popcorntime.android.data.api.dto.EztvTorrentDto
import com.popcorntime.android.data.api.dto.ShowDto
import com.popcorntime.android.data.api.dto.TvMazeShowDto
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.ShowFilter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Show/anime provider.
 *
 * **Browse** speaks the Butter / popcorn-ru API against the same mirror domains
 * popcorn-desktop uses, so both apps see identical catalogs and server-side
 * trending/sort/genre ordering:
 *
 *  - GET {base}shows/{page}?sort=..&order=-1&limit=50[&genre=..][&keywords=..][&anime=1]
 *    → JSON array of show summaries (~50/page, [] = end)
 *  - GET {base}show/{imdbId} → full show incl. episodes with embedded torrents
 *    (used as the detail fallback when TVMaze doesn't know the show)
 *
 * **Detail** stays on TVMaze + EZTV: TVMaze has richer episode metadata
 * (thumbnails, airstamps) and EZTV/Nyaa/Jackett supply fresher torrents.
 *
 *  - **TVMaze** (https://api.tvmaze.com) — show/episode metadata, free, no auth.
 *  - **EZTV** (https://eztv.re/api) — episode torrent magnet links indexed by IMDB ID.
 */
@Singleton
class ShowApiService @Inject constructor(
    private val client: HttpClient,
    @Named("showServers") private val servers: List<String>,
) {

    companion object {
        private const val TVMAZE = "https://api.tvmaze.com"
        private const val EZTV   = "https://eztv.re"
        private const val EZTV_PAGE_LIMIT = 100
        private const val EZTV_MAX_PAGES  = 3  // up to 300 torrents per show
        private const val PAGE_SIZE = 50

        /**
         * Maps a UI sort value to a Butter shows sort. Mirrors popcorn-desktop's
         * tv.js: "popularity" is deliberately sent as the API default "seeds"
         * (verified live: both produce identical ordering).
         */
        fun toButterShowSort(sortBy: String): String = when (sortBy.lowercase()) {
            "trending"   -> "trending"
            "popularity" -> "seeds"
            "updated"    -> "updated"
            "rating"     -> "rating"
            "year"       -> "year"
            else         -> "trending"
        }
    }

    // Rotate through mirrors on failure — same semantics as popcorn-desktop
    private val rotation = ServerRotation(servers)

    // ── Browse (popcorn-ru mirrors) ───────────────────────────────────────────

    /**
     * Returns a page of shows (or anime) from the popcorn-ru mirrors.
     *
     * Genre is only meaningful for shows — the anime catalog has no genre
     * filter (matching popcorn-desktop). Search uses the same endpoint via
     * the `keywords` param.
     */
    suspend fun listShows(filter: ShowFilter): List<ShowDto> = rotation.withRotation { base ->
        client.get("${base}shows/${filter.page}") {
            expectSuccess = true
            parameter("sort", toButterShowSort(filter.sortBy))
            parameter("order", filter.order)
            parameter("limit", PAGE_SIZE)
            if (filter.type == ContentType.ANIME) {
                parameter("anime", 1)
            } else if (filter.genre != "All Genre") {
                parameter("genre", filter.genre.lowercase())
            }
            if (filter.keywords.isNotBlank()) parameter("keywords", filter.keywords.trim())
        }.body()
    }

    /**
     * Full show detail (episodes + embedded torrents) from the popcorn-ru
     * mirrors — the fallback source when TVMaze doesn't know the show.
     */
    suspend fun getButterShowDetail(imdbId: String): ShowDto = rotation.withRotation { base ->
        client.get("${base}show/$imdbId") {
            expectSuccess = true
        }.body()
    }

    // ── Detail (TVMaze + EZTV) ────────────────────────────────────────────────

    /**
     * Returns the full show record (with episodes embedded) from TVMaze, plus
     * a flat list of EZTV torrents for that show's IMDB ID.
     */
    suspend fun getShowDetail(imdbId: String): ShowDetailResult {
        val show = lookupShow(imdbId)
        val eztvTorrents = fetchEztvTorrents(show.externals?.imdb ?: imdbId)
        return ShowDetailResult(show, eztvTorrents)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun lookupShow(imdbId: String): TvMazeShowDto {
        val base: TvMazeShowDto = if (imdbId.startsWith("tvmaze:")) {
            // Synthetic ID from an old build's TVMaze browse (kept for legacy library items)
            val tvId = imdbId.removePrefix("tvmaze:")
            client.get("$TVMAZE/shows/$tvId").body()
        } else {
            client.get("$TVMAZE/lookup/shows") { parameter("imdb", imdbId) }.body()
        }
        // Re-fetch with episodes embedded (the lookup endpoint doesn't embed them)
        return client.get("$TVMAZE/shows/${base.id}") {
            parameter("embed", "episodes")
        }.body()
    }

    private suspend fun fetchEztvTorrents(imdbId: String): List<EztvTorrentDto> {
        if (!imdbId.startsWith("tt")) return emptyList()
        // EZTV expects a numeric IMDB ID without leading zeros
        val numericId = imdbId.removePrefix("tt").trimStart('0').ifBlank { "0" }
        val all = mutableListOf<EztvTorrentDto>()
        try {
            for (page in 1..EZTV_MAX_PAGES) {
                val resp = client.get("$EZTV/api/get-torrents") {
                    parameter("imdb_id", numericId)
                    parameter("limit",   EZTV_PAGE_LIMIT)
                    parameter("page",    page)
                }.body<EztvResponse>()
                all += resp.torrents
                if (resp.torrents.size < EZTV_PAGE_LIMIT) break
            }
        } catch (e: Exception) {
            Timber.w(e, "ShowApiService: EZTV fetch failed for $imdbId")
        }
        return all
    }
}

// ── Return type ───────────────────────────────────────────────────────────────

data class ShowDetailResult(
    val show: TvMazeShowDto,
    val eztvTorrents: List<EztvTorrentDto>,
)
