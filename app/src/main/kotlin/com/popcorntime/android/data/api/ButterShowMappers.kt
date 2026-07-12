package com.popcorntime.android.data.api

import com.popcorntime.android.data.api.dto.EpisodeDto
import com.popcorntime.android.data.api.dto.ShowDto
import com.popcorntime.android.domain.model.Episode
import com.popcorntime.android.domain.model.EpisodeTorrent
import com.popcorntime.android.domain.model.Show

/**
 * Mappers for the Butter / popcorn-ru shows API.
 * Kept top-level (not private inside the repository) so they are unit-testable.
 * Reuses [unescapeHtmlAmpersands] and [toHttps] from ButterMappers.kt.
 */

/**
 * Browse-grid mapping. The browse response is a summary object — no synopsis,
 * genres, status, or episodes; those arrive with the detail fetch.
 *
 * Anime titles come back in their original script (often Japanese), so like
 * popcorn-desktop we derive the display title from the latin slug.
 */
fun ShowDto.toSummaryDomain(isAnime: Boolean): Show = Show(
    imdbId      = imdbId.ifBlank { id },
    tvdbId      = tvdbId,
    title       = displayTitle(isAnime),
    year        = year,
    slug        = slug,
    synopsis    = synopsis,
    runtime     = runtime,
    country     = country,
    network     = network,
    airDay      = airDay,
    airTime     = airTime,
    status      = status,
    numSeasons  = numSeasons,
    rating      = rating.percentage / 10.0,
    genres      = genres.map { it.titlecaseWords() },
    posterUrl   = images.poster.toHttps(),
    backdropUrl = images.fanart.ifBlank { images.poster }.toHttps(),
    bannerUrl   = images.banner.toHttps(),
    episodes    = emptyList(),
)

/**
 * Detail mapping — everything from the summary plus episodes with their
 * embedded per-quality torrents. Used as the fallback when TVMaze doesn't
 * know the show (mostly deep-catalog anime).
 *
 * The popcorn API has no episode thumbnails; blank URLs render the existing
 * icon placeholder in the episode row.
 */
fun ShowDto.toDetailDomain(isAnime: Boolean): Show =
    toSummaryDomain(isAnime).copy(
        episodes = episodes
            .filter { it.season > 0 || it.episode > 0 }  // drop fully-unnumbered specials
            .map { it.toDomain() },
    )

private fun EpisodeDto.toDomain(): Episode = Episode(
    // Some mirror entries lack a tvdb id; synthesize a stable unique key so
    // the episode list's LazyColumn keys never collide.
    tvdbId       = tvdbId.takeIf { it != 0 } ?: (season * 10_000 + episode),
    season       = season,
    episode      = episode,
    title        = title,
    overview     = overview,
    firstAired   = firstAired,
    thumbnailUrl = "",
    torrents     = torrents.mapValues { (_, t) ->
        EpisodeTorrent(
            url      = unescapeHtmlAmpersands(t.url),
            seeds    = t.seeds,
            peers    = t.peers,
            provider = t.provider,
        )
    },
)

private fun ShowDto.displayTitle(isAnime: Boolean): String =
    if (isAnime && slug.isNotBlank()) slugToTitle(slug) else title

/** "jujutsu-kaisen" → "Jujutsu Kaisen" — mirrors popcorn-desktop's anime title handling. */
fun slugToTitle(slug: String): String =
    slug.split('-')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }

/** "action & adventure" → "Action & Adventure" */
private fun String.titlecaseWords(): String =
    split(" ").joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }
