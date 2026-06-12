package com.popcorntime.android.domain.model

data class MovieFilter(
    val page: Int = 1,
    val genre: String = "All",
    val sortBy: String = "last added",
    val orderBy: String = "desc",
    val quality: String = "All",
    val minimumRating: Int = 0,
    val queryTerm: String = "",
)

/** Sort options matching the Butter / popcorn-ru API (same as popcorn-desktop). */
enum class SortOption(val apiValue: String, val label: String) {
    TRENDING("trending", "Trending"),
    POPULARITY("popularity", "Popularity"),
    LAST_ADDED("last added", "Last Added"),
    YEAR("year", "Year"),
    TITLE("title", "Title"),
    RATING("rating", "Rating"),
}

/**
 * Genres supported by the Butter / popcorn-ru API (TMDB-style names).
 * They are lowercased before being sent as the `genre` query parameter.
 */
val ALL_GENRES = listOf(
    "All", "Action", "Adventure", "Animation", "Comedy", "Crime",
    "Documentary", "Drama", "Family", "Fantasy", "History", "Horror",
    "Music", "Mystery", "Romance", "Science Fiction", "Thriller",
    "War", "Western",
)

val ALL_QUALITIES = listOf("All", "720p", "1080p", "2160p")
