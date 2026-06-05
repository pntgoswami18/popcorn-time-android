package com.popcorntime.android.domain.model

data class MovieFilter(
    val page: Int = 1,
    val genre: String = "All",
    val sortBy: String = "date_added",
    val orderBy: String = "desc",
    val quality: String = "All",
    val minimumRating: Int = 0,
    val queryTerm: String = "",
)

enum class SortOption(val apiValue: String, val label: String) {
    LAST_ADDED("date_added", "Last Added"),
    TRENDING("download_count", "Trending"),
    POPULARITY("like_count", "Popularity"),
    YEAR("year", "Year"),
    TITLE("title", "Title"),
    RATING("rating", "Rating"),
}

val ALL_GENRES = listOf(
    "All", "Action", "Adventure", "Animation", "Biography", "Comedy",
    "Crime", "Documentary", "Drama", "Family", "Fantasy", "Film-Noir",
    "History", "Horror", "Music", "Musical", "Mystery", "Romance",
    "Sci-Fi", "Short", "Sport", "Thriller", "War", "Western",
)

val ALL_QUALITIES = listOf("All", "720p", "1080p", "2160p", "3D")
