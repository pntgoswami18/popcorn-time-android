package com.popcorntime.android.data.repository

import com.popcorntime.android.data.api.JackettApiService
import com.popcorntime.android.data.api.ShowApiService
import com.popcorntime.android.data.api.ShowDetailResult
import com.popcorntime.android.data.api.dto.EztvTorrentDto
import com.popcorntime.android.data.api.dto.TvMazeEpisodeDto
import com.popcorntime.android.data.api.dto.TvMazeShowDto
import com.popcorntime.android.data.api.toEpisodeTorrent
import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import com.popcorntime.android.data.sources.TorrentSourcePrefs
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.Episode
import com.popcorntime.android.domain.model.EpisodeTorrent
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.domain.model.ShowFilter
import com.popcorntime.android.domain.model.TorrentSource
import com.popcorntime.android.domain.repository.ShowRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowRepositoryImpl @Inject constructor(
    private val api: ShowApiService,
    private val watchedDao: WatchedDao,
    private val bookmarkedDao: BookmarkedDao,
    private val sourcePrefs: TorrentSourcePrefs,
    private val jackettApi: JackettApiService,
) : ShowRepository {

    override suspend fun getShows(filter: ShowFilter): Result<List<Show>> {
        return try {
            Result.success(api.listShows(filter).map { it.toSummaryDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getShowDetail(imdbId: String, type: ContentType): Result<Show> {
        return try {
            val result = api.getShowDetail(imdbId, type)
            if (sourcePrefs.getShowSource() == TorrentSource.JACKETT) {
                val baseUrl = sourcePrefs.getJackettUrl()
                val apiKey = sourcePrefs.getJackettApiKey()
                if (baseUrl.isNotBlank() && apiKey.isNotBlank()) {
                    val jackettResults = try {
                        withTimeout(15_000L) {
                            jackettApi.searchShows(
                                query = result.show.name,
                                season = null,
                                episode = null,
                                apiKey = apiKey,
                                baseUrl = baseUrl,
                            )
                        }
                    } catch (e: TimeoutCancellationException) {
                        Timber.w("ShowRepositoryImpl: Jackett show search timed out, falling back to EZTV")
                        emptyList()
                    }
                    if (jackettResults.isNotEmpty()) {
                        val sePattern = Regex("S(\\d{2})E(\\d{2})", RegexOption.IGNORE_CASE)
                        val torrentIndex = mutableMapOf<Int, MutableMap<Int, MutableMap<String, EpisodeTorrent>>>()
                        for (dto in jackettResults) {
                            val match = sePattern.find(dto.title) ?: continue
                            val season = match.groupValues[1].toIntOrNull() ?: continue
                            val episode = match.groupValues[2].toIntOrNull() ?: continue
                            val quality = dto.title.lowercase().let { t ->
                                when {
                                    "2160p" in t || "4k" in t || "uhd" in t -> "4K"
                                    "1080p" in t -> "1080p"
                                    "720p" in t -> "720p"
                                    "480p" in t -> "480p"
                                    else -> "720p"
                                }
                            }
                            val ep = dto.toEpisodeTorrent()
                            val byQuality = torrentIndex.getOrPut(season) { mutableMapOf() }.getOrPut(episode) { mutableMapOf() }
                            val existing = byQuality[quality]
                            if (existing == null || ep.seeds > existing.seeds) {
                                byQuality[quality] = ep
                            }
                        }
                        return Result.success(result.toDomain(torrentIndex))
                    }
                }
            }
            Result.success(result.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeWatched(): Flow<Set<String>> =
        watchedDao.observeAll().map { it.toSet() }

    override fun observeBookmarked(): Flow<Set<String>> =
        bookmarkedDao.observeAll().map { it.toSet() }

    override suspend fun toggleWatched(imdbId: String) {
        if (watchedDao.isWatched(imdbId)) watchedDao.delete(imdbId)
        else watchedDao.insert(WatchedEntity(imdbId))
    }

    override suspend fun toggleBookmarked(imdbId: String) {
        if (bookmarkedDao.isBookmarked(imdbId)) bookmarkedDao.delete(imdbId)
        else bookmarkedDao.insert(BookmarkedEntity(imdbId))
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

/**
 * Lightweight summary used by the browse grid — no episodes, no EZTV lookup.
 * Uses TVMaze ID as synthetic IMDB for shows that lack one (rare).
 */
private fun TvMazeShowDto.toSummaryDomain() = Show(
    imdbId     = externals?.imdb ?: "tvmaze:$id",
    tvdbId     = externals?.thetvdb?.toString() ?: "",
    title      = name,
    year       = premiered?.take(4) ?: "",
    slug       = name.slugify(),
    synopsis   = summary?.stripHtml() ?: "",
    runtime    = runtime?.toString() ?: averageRuntime?.toString() ?: "",
    country    = network?.country?.name ?: webChannel?.country?.name ?: "",
    network    = network?.name ?: webChannel?.name ?: "",
    airDay     = "",
    airTime    = "",
    status     = status,
    numSeasons = 0,
    rating     = rating.average ?: 0.0,
    genres     = genres,
    posterUrl  = image?.original ?: image?.medium ?: "",
    backdropUrl = image?.original ?: "",
    bannerUrl  = image?.medium ?: "",
    episodes   = emptyList(),
)

/** Full detail mapping including EZTV episode torrents (or a pre-built index from Jackett). */
private fun ShowDetailResult.toDomain(
    overrideTorrentIndex: Map<Int, Map<Int, Map<String, EpisodeTorrent>>>? = null,
): Show {
    val tvMazeShow = show
    val imdbId = tvMazeShow.externals?.imdb ?: "tvmaze:${tvMazeShow.id}"

    // season → episode → quality → best-seed EztvTorrentDto
    val torrentIndex = overrideTorrentIndex ?: buildTorrentIndex(eztvTorrents)

    val episodes = tvMazeShow.embedded?.episodes.orEmpty()
        .filter { it.number != null }           // skip unnumbered specials
        .map { ep -> ep.toDomain(torrentIndex) }

    return Show(
        imdbId     = imdbId,
        tvdbId     = tvMazeShow.externals?.thetvdb?.toString() ?: "",
        title      = tvMazeShow.name,
        year       = tvMazeShow.premiered?.take(4) ?: "",
        slug       = tvMazeShow.name.slugify(),
        synopsis   = tvMazeShow.summary?.stripHtml() ?: "",
        runtime    = tvMazeShow.runtime?.toString() ?: tvMazeShow.averageRuntime?.toString() ?: "",
        country    = tvMazeShow.network?.country?.name ?: tvMazeShow.webChannel?.country?.name ?: "",
        network    = tvMazeShow.network?.name ?: tvMazeShow.webChannel?.name ?: "",
        airDay     = "",
        airTime    = "",
        status     = tvMazeShow.status,
        numSeasons = episodes.maxOfOrNull { it.season } ?: 0,
        rating     = tvMazeShow.rating.average ?: 0.0,
        genres     = tvMazeShow.genres,
        posterUrl  = tvMazeShow.image?.original ?: tvMazeShow.image?.medium ?: "",
        backdropUrl = tvMazeShow.image?.original ?: "",
        bannerUrl  = tvMazeShow.image?.medium ?: "",
        episodes   = episodes,
    )
}

private fun TvMazeEpisodeDto.toDomain(
    torrentIndex: Map<Int, Map<Int, Map<String, EpisodeTorrent>>>,
): Episode {
    val epTorrents = torrentIndex[season]?.get(number!!) ?: emptyMap()
    return Episode(
        tvdbId    = id,
        season    = season,
        episode   = number!!,
        title     = name,
        overview  = summary?.stripHtml() ?: "",
        firstAired = airdate.toUnixSeconds(),
        torrents  = epTorrents,
    )
}

/**
 * Builds a 3-level index: season → episode → quality → best-seeded EpisodeTorrent.
 * Quality is inferred from the EZTV filename (4K / 1080p / 720p / 480p / SD).
 */
private fun buildTorrentIndex(
    eztvTorrents: List<EztvTorrentDto>,
): Map<Int, Map<Int, Map<String, EpisodeTorrent>>> {
    // Accumulate best torrent per (season, episode, quality)
    val best = mutableMapOf<Triple<Int, Int, String>, EztvTorrentDto>()

    for (t in eztvTorrents) {
        val season  = t.season.toIntOrNull()  ?: continue
        val episode = t.episode.toIntOrNull() ?: continue
        val quality = t.filename.detectQuality()
        val key = Triple(season, episode, quality)
        if ((best[key]?.seeds ?: -1) < t.seeds) best[key] = t
    }

    // Restructure to season → episode → quality → EpisodeTorrent
    val result = mutableMapOf<Int, MutableMap<Int, MutableMap<String, EpisodeTorrent>>>()
    for ((key, t) in best) {
        val (season, episode, quality) = key
        result
            .getOrPut(season)  { mutableMapOf() }
            .getOrPut(episode) { mutableMapOf() }[quality] = EpisodeTorrent(
                url      = t.magnetUrl,
                seeds    = t.seeds,
                peers    = t.peers,
                provider = "EZTV",
            )
    }
    return result
}

// ── String utilities ──────────────────────────────────────────────────────────

private fun String.stripHtml(): String =
    replace(Regex("<[^>]*>"), "").trim()

private fun String.slugify(): String =
    lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

/** Infer video quality from an EZTV filename. */
private fun String.detectQuality(): String {
    val fn = this.lowercase()
    return when {
        "2160p" in fn || "4k" in fn || "uhd" in fn -> "4K"
        "1080p" in fn                               -> "1080p"
        "720p"  in fn                               -> "720p"
        "480p"  in fn                               -> "480p"
        else                                        -> "SD"
    }
}

/** Parse "YYYY-MM-DD" → Unix timestamp in seconds (0 on failure), always UTC. */
private fun String.toUnixSeconds(): Long {
    if (isBlank()) return 0L
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        sdf.parse(this)?.time?.div(1000) ?: 0L
    } catch (_: Exception) { 0L }
}
