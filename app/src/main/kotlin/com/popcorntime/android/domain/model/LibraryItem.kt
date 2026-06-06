package com.popcorntime.android.domain.model

data class LibraryItem(
    val imdbId: String,
    val title: String,
    val posterUrl: String,
    val year: String,
    val contentType: LibraryContentType,
    val addedAt: Long,
)

enum class LibraryContentType { MOVIE, SHOW }
