package com.popcorntime.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.popcorntime.android.data.trakt.TraktAuthService
import com.popcorntime.android.data.trakt.TraktSyncService
import com.popcorntime.android.data.trakt.TraktTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import com.popcorntime.android.BuildConfig
import javax.inject.Named
import javax.inject.Singleton

private val Context.traktDataStore: DataStore<Preferences> by preferencesDataStore(name = "trakt_prefs")

@Module
@InstallIn(SingletonComponent::class)
object TraktModule {

    @Provides
    @Singleton
    @Named("traktDataStore")
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.traktDataStore

    @Provides
    @Singleton
    fun provideTraktTokenStore(@Named("traktDataStore") dataStore: DataStore<Preferences>): TraktTokenStore =
        TraktTokenStore(dataStore)

    @Provides
    @Singleton
    @Named("trakt")
    fun provideTraktHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.trakt.tv"
            }
            headers.append("trakt-api-version", "2")
            headers.append("trakt-api-key", BuildConfig.TRAKT_CLIENT_ID)
        }
    }

    @Provides
    @Singleton
    fun provideTraktAuthService(@Named("trakt") client: HttpClient): TraktAuthService =
        TraktAuthService(client, BuildConfig.TRAKT_CLIENT_ID)

    @Provides
    @Singleton
    fun provideTraktSyncService(
        @Named("trakt") client: HttpClient,
        tokenStore: TraktTokenStore,
    ): TraktSyncService = TraktSyncService(client, tokenStore)
}
