package com.popcorntime.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.popcorntime.android.data.api.JackettApiService
import com.popcorntime.android.data.sources.TorrentSourcePrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Named
import javax.inject.Singleton

private val Context.sourceDataStore: DataStore<Preferences> by preferencesDataStore(name = "source_prefs")

@Module
@InstallIn(SingletonComponent::class)
object SourceModule {

    @Provides
    @Singleton
    @Named("sourceDataStore")
    fun provideSourceDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        ctx.sourceDataStore

    @Provides
    @Singleton
    fun provideTorrentSourcePrefs(
        @Named("sourceDataStore") ds: DataStore<Preferences>,
    ): TorrentSourcePrefs = TorrentSourcePrefs(ds)

    @Provides
    @Singleton
    fun provideJackettApiService(client: HttpClient): JackettApiService =
        JackettApiService(client)
}
