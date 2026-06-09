package com.popcorntime.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.popcorntime.android.data.preferences.BrowserPrefsStore
import com.popcorntime.android.data.preferences.ThemePrefsStore
import com.popcorntime.android.data.preferences.TorrentPrefsStore
import com.popcorntime.android.data.subtitles.SubtitleStyleStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserModule {

    @Provides
    @Singleton
    @Named("browserPrefsDataStore")
    fun provideBrowserPrefsDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("browser_prefs") }

    @Provides
    @Singleton
    fun provideBrowserPrefsStore(
        @Named("browserPrefsDataStore") ds: DataStore<Preferences>,
    ): BrowserPrefsStore = BrowserPrefsStore(ds)

    @Provides
    @Singleton
    @Named("torrentPrefsDataStore")
    fun provideTorrentPrefsDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("torrent_prefs") }

    @Provides
    @Singleton
    fun provideTorrentPrefsStore(
        @Named("torrentPrefsDataStore") ds: DataStore<Preferences>,
    ): TorrentPrefsStore = TorrentPrefsStore(ds)

    @Provides
    @Singleton
    @Named("themeDataStore")
    fun provideThemeDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("theme_prefs") }

    @Provides
    @Singleton
    fun provideThemePrefsStore(
        @Named("themeDataStore") ds: DataStore<Preferences>,
    ): ThemePrefsStore = ThemePrefsStore(ds)

    @Provides
    @Singleton
    @Named("subtitleStyleDataStore")
    fun provideSubtitleStyleDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("subtitle_style_prefs") }

    @Provides
    @Singleton
    fun provideSubtitleStyleStore(
        @Named("subtitleStyleDataStore") ds: DataStore<Preferences>,
    ): SubtitleStyleStore = SubtitleStyleStore(ds)
}
