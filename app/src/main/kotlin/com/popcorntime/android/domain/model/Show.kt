package com.popcorntime.android.domain.model

data class Show(
    val imdbId: String,
    val tvdbId: String,
    val title: String,
    val year: String,
    val slug: String,
    val synopsis: String,
    val runtime: String,
    val country: String,
    val network: String,
    val airDay: String,
    val airTime: String,
    val status: String,
    val numSeasons: Int,
    val rating: Double,
    val genres: List<String>,
    val posterUrl: String,
    val backdropUrl: String,
    val bannerUrl: String,
    val episodes: List<Episode>,
    val isWatched: Boolean = false,
    val isBookmarked: Boolean = false,
)

data class Episode(
    val tvdbId: Int,
    val season: Int,
    val episode: Int,
    val title: String,
    val overview: String,
    val firstAired: Long,
    val thumbnailUrl: String,
    val torrents: Map<String, EpisodeTorrent>,  // quality -> torrent
)

data class EpisodeTorrent(
    val url: String,
    val seeds: Int,
    val peers: Int,
    val provider: String,
)

/** Groups episodes into seasons for the UI. */
data class Season(
    val number: Int,
    val episodes: List<Episode>,
)

fun Show.seasons(): List<Season> =
    episodes
        .groupBy { it.season }
        .map { (num, eps) -> Season(num, eps.sortedBy { it.episode }) }
        .sortedBy { it.number }

data class ShowFilter(
    val page: Int = 1,
    val genre: String = "All Genre",
    val sortBy: String = "trending",
    val order: Int = -1,
    val keywords: String = "",
    val type: ContentType = ContentType.SHOW,
)

enum class ContentType { SHOW, ANIME }

/**
 * TV genres served by the popcorn-ru shows API (from its shows/stat endpoint).
 * Sent lowercase as the `genre=` param; the API matches case-insensitively.
 */
val SHOW_GENRES = listOf(
    "All Genre", "Action & Adventure", "Animation", "Comedy", "Crime",
    "Documentary", "Drama", "Family", "Kids", "Mystery", "News", "Reality",
    "Sci-Fi & Fantasy", "Soap", "Talk", "War & Politics", "Western",
)
