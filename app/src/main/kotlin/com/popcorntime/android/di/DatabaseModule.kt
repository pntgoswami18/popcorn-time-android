package com.popcorntime.android.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.popcorntime.android.data.db.AppDatabase
import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.DownloadDao
import com.popcorntime.android.data.db.dao.LibraryItemDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.dao.WatchlistDao
import com.popcorntime.android.data.repository.LibraryRepositoryImpl
import com.popcorntime.android.data.repository.MovieRepositoryImpl
import com.popcorntime.android.data.repository.ShowRepositoryImpl
import com.popcorntime.android.domain.repository.LibraryRepository
import com.popcorntime.android.domain.repository.MovieRepository
import com.popcorntime.android.domain.repository.ShowRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Migration from v1 → v2: adds the `library_items` table.
 *
 * v1 had: watched, bookmarked, watchlist
 * v2 adds: library_items (persists downloaded content for the Library tab)
 *
 * Using an explicit migration rather than fallbackToDestructiveMigration
 * ensures existing watched/bookmarked/watchlist data survives app updates.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `library_items` (
                `imdbId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `posterUrl` TEXT NOT NULL,
                `year` TEXT NOT NULL,
                `contentType` TEXT NOT NULL,
                `addedAt` INTEGER NOT NULL,
                PRIMARY KEY(`imdbId`)
            )"""
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `downloads` (
                `imdbId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `magnetUrl` TEXT NOT NULL,
                `filePath` TEXT,
                `completedAt` INTEGER,
                PRIMARY KEY(`imdbId`)
            )"""
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "popcorntime.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun provideWatchedDao(db: AppDatabase): WatchedDao = db.watchedDao()
    @Provides fun provideBookmarkedDao(db: AppDatabase): BookmarkedDao = db.bookmarkedDao()
    @Provides fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
    @Provides fun provideLibraryItemDao(db: AppDatabase): LibraryItemDao = db.libraryItemDao()
    @Provides fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds @Singleton
    abstract fun bindShowRepository(impl: ShowRepositoryImpl): ShowRepository

    @Binds @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository
}
