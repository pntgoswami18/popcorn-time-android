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
