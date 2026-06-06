package com.popcorntime.android.di

import android.content.Context
import androidx.room.Room
import com.popcorntime.android.data.db.AppDatabase
import com.popcorntime.android.data.db.dao.BookmarkedDao
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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "popcorntime.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideWatchedDao(db: AppDatabase): WatchedDao = db.watchedDao()
    @Provides fun provideBookmarkedDao(db: AppDatabase): BookmarkedDao = db.bookmarkedDao()
    @Provides fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
    @Provides fun provideLibraryItemDao(db: AppDatabase): LibraryItemDao = db.libraryItemDao()
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
