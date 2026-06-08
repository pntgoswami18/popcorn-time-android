package com.popcorntime.android.data.remote

import com.popcorntime.android.domain.model.LibraryContentType
import kotlinx.serialization.Serializable

@Serializable
data class QueueItem(
    val imdbId: String,
    val title: String = "",
    val quality: String = "1080p",
    val contentType: LibraryContentType = LibraryContentType.MOVIE,
    val season: Int? = null,
    val episode: Int? = null,
    val magnet: String = "",
)
