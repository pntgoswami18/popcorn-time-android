package com.popcorntime.android.data.sources

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.popcorntime.android.domain.model.TorrentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Singleton
class TorrentSourcePrefs constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val movieSourceKey = stringPreferencesKey("active_movie_source")
    private val showSourceKey = stringPreferencesKey("active_show_source")
    private val jackettUrlKey = stringPreferencesKey("jackett_url")
    private val jackettApiKeyKey = stringPreferencesKey("jackett_api_key")

    suspend fun getMovieSource(): TorrentSource =
        TorrentSource.valueOf(dataStore.data.first()[movieSourceKey] ?: TorrentSource.YTS.name)

    suspend fun getShowSource(): TorrentSource =
        TorrentSource.valueOf(dataStore.data.first()[showSourceKey] ?: TorrentSource.EZTV.name)

    suspend fun setMovieSource(source: TorrentSource) {
        dataStore.edit { it[movieSourceKey] = source.name }
    }

    suspend fun setShowSource(source: TorrentSource) {
        dataStore.edit { it[showSourceKey] = source.name }
    }

    suspend fun getJackettUrl(): String =
        dataStore.data.first()[jackettUrlKey] ?: ""

    suspend fun getJackettApiKey(): String =
        dataStore.data.first()[jackettApiKeyKey] ?: ""

    suspend fun saveJackettConfig(url: String, apiKey: String) {
        dataStore.edit { prefs ->
            prefs[jackettUrlKey] = url
            prefs[jackettApiKeyKey] = apiKey
        }
    }

    fun observeMovieSource(): Flow<TorrentSource> =
        dataStore.data.map { prefs ->
            TorrentSource.valueOf(prefs[movieSourceKey] ?: TorrentSource.YTS.name)
        }

    fun observeShowSource(): Flow<TorrentSource> =
        dataStore.data.map { prefs ->
            TorrentSource.valueOf(prefs[showSourceKey] ?: TorrentSource.EZTV.name)
        }
}
