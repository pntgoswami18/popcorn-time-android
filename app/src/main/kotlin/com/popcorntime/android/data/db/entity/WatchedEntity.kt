package com.popcorntime.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched")
data class WatchedEntity(
    @PrimaryKey val imdbId: String,
    val watchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bookmarked")
data class BookmarkedEntity(
    @PrimaryKey val imdbId: String,
    val bookmarkedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val imdbId: String,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey val imdbId: String,
    val title: String,
    val posterUrl: String,
    val year: String,
    val contentType: String,   // "movie" or "show"
    val addedAt: Long = System.currentTimeMillis(),
)
