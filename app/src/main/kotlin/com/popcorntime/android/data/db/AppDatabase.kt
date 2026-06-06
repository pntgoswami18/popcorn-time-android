package com.popcorntime.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.LibraryItemDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.dao.WatchlistDao
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.LibraryItemEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import com.popcorntime.android.data.db.entity.WatchlistEntity

@Database(
    entities = [WatchedEntity::class, BookmarkedEntity::class, WatchlistEntity::class, LibraryItemEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchedDao(): WatchedDao
    abstract fun bookmarkedDao(): BookmarkedDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun libraryItemDao(): LibraryItemDao
}
