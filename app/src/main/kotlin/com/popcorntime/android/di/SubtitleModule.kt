package com.popcorntime.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.popcorntime.android.data.subtitles.OsAuthService
import com.popcorntime.android.data.subtitles.OsTokenStore
import com.popcorntime.android.data.subtitles.SubtitleService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Named
import javax.inject.Singleton

private val Context.osDataStore: DataStore<Preferences> by preferencesDataStore(name = "os_prefs")

@Module
@InstallIn(SingletonComponent::class)
object SubtitleModule {

    @Provides
    @Singleton
    @Named("osDataStore")
    fun provideOsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.osDataStore

    @Provides
    @Singleton
    fun provideOsTokenStore(@Named("osDataStore") dataStore: DataStore<Preferences>): OsTokenStore =
        OsTokenStore(dataStore)

    @Provides
    @Singleton
    fun provideOsAuthService(client: HttpClient): OsAuthService =
        OsAuthService(client)

    @Provides
    @Singleton
    fun provideSubtitleService(client: HttpClient, osTokenStore: OsTokenStore): SubtitleService =
        SubtitleService(client, osTokenStore)
}
