package com.popcorntime.android.di

import android.content.Context
import com.popcorntime.android.data.torrent.TorrentStreamServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Default API servers — matches popcorn-desktop settings.js
    private val DEFAULT_MOVIE_SERVERS = listOf(
        "https://fusme.link/",
        "https://jfper.link/",
        "https://uxert.link/",
        "https://yrkde.link/",
        "https://yts.bz/",
    )

    // Same mirrors minus yts.bz, which is a movies-only API and does not answer
    // shows/ requests at all (connection failure — verified live).
    private val DEFAULT_SHOW_SERVERS = listOf(
        "https://fusme.link/",
        "https://jfper.link/",
        "https://uxert.link/",
        "https://yrkde.link/",
    )

    @Provides
    @Singleton
    @Named("movieServers")
    fun provideMovieServers(): List<@JvmSuppressWildcards String> = DEFAULT_MOVIE_SERVERS

    @Provides
    @Singleton
    @Named("showServers")
    fun provideShowServers(): List<@JvmSuppressWildcards String> = DEFAULT_SHOW_SERVERS

    @Provides
    @Singleton
    fun provideTorrentCacheDir(@ApplicationContext context: Context): File =
        File(context.cacheDir, "torrents").also { it.mkdirs() }

    @Provides
    @Singleton
    fun provideTorrentStreamServer(): TorrentStreamServer = TorrentStreamServer()
}
