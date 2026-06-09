package com.popcorntime.android.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val imdbId: String,
    val title: String,
    val magnetUrl: String,
    val filePath: String? = null,
    val completedAt: Long? = null,
)
