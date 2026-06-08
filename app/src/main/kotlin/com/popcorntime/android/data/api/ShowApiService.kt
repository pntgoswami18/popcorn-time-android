package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.EztvResponse
import com.popcorntime.android.data.api.dto.EztvTorrentDto
import com.popcorntime.android.data.api.dto.TvMazeSearchResultDto
import com.popcorntime.android.data.api.dto.TvMazeShowDto
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.ShowFilter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches TV shows and anime using two public, maintenance-free APIs:
 *
 *  - **TVMaze** (https://api.tvmaze.com) — show/episode metadata, free, no auth.
 *  - **EZTV** (https://eztv.re/api) — episode torrent magnet links indexed by IMDB ID.
 *
 * The old Butter/api-fetch.sh servers are permanently offline as of 2025.
 */
@Singleton
class ShowApiService @Inject constructor(private val client: HttpClient) {

    companion object {
        private const val TVMAZE = "https://api.tvmaze.com"
        private const val EZTV   = "https://eztv.re"
        private const val EZTV_PAGE_LIMIT = 100
        private const val EZTV_MAX_PAGES  = 3  // up to 300 torrents per show
    }

    // ── List ──────────────────────────────────────────────────────────────────

    /**
     * Returns a page of shows (or anime) from TVMaze.
     *
     * TVMaze `/shows?page=N` is 0-indexed and returns ~250 entries per page.
     * Our ShowFilter pages are 1-indexed, so we subtract 1.
     *
     * Anime = TVMaze type "Animation" or genre "Anime".
     */
    suspend fun listShows(filter: ShowFilter): List<TvMazeShowDto> {
        val isAnime = filter.type == ContentType.ANIME
        return if (filter.keywords.isNotBlank()) {
            client.get("$TVMAZE/search/shows") {
                parameter("q", filter.keywords)
            }.body<List<TvMazeSearchResultDto>>()
                .map { it.show }
                .filter { if (isAnime) it.isAnimation() else !it.isAnimation() }
        } else {
            val tvmazePage = (filter.page - 1).coerceAtLeast(0)
            client.get("$TVMAZE/shows") {
                parameter("page", tvmazePage)
            }.body<List<TvMazeShowDto>>()
                .filter { if (isAnime) it.isAnimation() else !it.isAnimation() }
                .let { shows ->
                    if (filter.genre != "All")
                        shows.filter { filter.genre in it.genres }
                    else shows
                }
        }
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    /**
     * Returns the full show record (with episodes embedded) from TVMaze, plus
     * a flat list of EZTV torrents for that show's IMDB ID.
     */
    suspend fun getShowDetail(imdbId: String, type: ContentType): ShowDetailResult {
        val show = lookupShow(imdbId)
        val eztvTorrents = fetchEztvTorrents(show.externals?.imdb ?: imdbId)
        return ShowDetailResult(show, eztvTorrents)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun lookupShow(imdbId: String): TvMazeShowDto {
        val base: TvMazeShowDto = if (imdbId.startsWith("tvmaze:")) {
            // Synthetic ID we generated for shows that had no IMDB entry
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

// ── Extension helper ──────────────────────────────────────────────────────────

internal fun TvMazeShowDto.isAnimation(): Boolean =
    type.equals("Animation", ignoreCase = true) ||
    genres.any { it.equals("Anime", ignoreCase = true) ||
                 it.equals("Animation", ignoreCase = true) }
