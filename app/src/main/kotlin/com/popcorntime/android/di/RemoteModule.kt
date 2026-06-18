package com.popcorntime.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import android.os.SystemClock
import com.popcorntime.android.data.remote.PairingManager
import com.popcorntime.android.data.remote.RemoteControlServer
import com.popcorntime.android.data.remote.RemoteControlServerController
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

    @Provides
    @Singleton
    fun providePairingManager(tokenStore: RemoteControlTokenStore): PairingManager =
        PairingManager(
            clock = { SystemClock.elapsedRealtime() },
            issueToken = { clientName -> tokenStore.issueSessionToken(clientName) },
            revokeToken = { token -> tokenStore.revokeSessionToken(token) },
        )

    @Provides
    @Singleton
    fun provideRemoteControlServerController(
        tokenStore: RemoteControlTokenStore,
        server: RemoteControlServer,
    ): RemoteControlServerController = RemoteControlServerController(
        enabled = tokenStore.observeEnabled(),
        getToken = { tokenStore.getOrCreateToken() },
        startServer = { token -> server.startIfNotRunning(token) },
        stopServer = { server.stopIfRunning() },
    )

    // PlaybackController, PlaybackQueue, RemoteControlServer are @Singleton @Inject constructor — no @Provides needed
}
