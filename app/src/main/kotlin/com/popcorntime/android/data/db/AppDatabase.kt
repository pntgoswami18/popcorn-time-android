package com.popcorntime.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.WatchedEntity

@Database(
    entities = [WatchedEntity::class, BookmarkedEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchedDao(): WatchedDao
    abstract fun bookmarkedDao(): BookmarkedDao
}
