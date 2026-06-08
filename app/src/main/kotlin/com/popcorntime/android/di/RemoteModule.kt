package com.popcorntime.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.popcorntime.android.data.remote.RemoteControlTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

private val Context.remoteDataStore: DataStore<Preferences> by preferencesDataStore(name = "remote_prefs")

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Provides
    @Singleton
    @Named("remoteDataStore")
    fun provideRemoteDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.remoteDataStore

    @Provides
    @Singleton
    fun provideRemoteControlTokenStore(
        @Named("remoteDataStore") dataStore: DataStore<Preferences>,
    ): RemoteControlTokenStore = RemoteControlTokenStore(dataStore)

    // PlaybackController, PlaybackQueue, RemoteControlServer are @Singleton @Inject constructor — no @Provides needed
}
