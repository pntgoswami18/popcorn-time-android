package com.popcorntime.android.data.player

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PlaybackPositionStore @Inject constructor(
    @Named("playerDataStore") val prefs: DataStore<Preferences>,
) {
    suspend fun savePosition(key: String, positionMs: Long) {
        prefs.edit { it[longPreferencesKey(key)] = positionMs }
    }

    suspend fun getPosition(key: String): Long =
        prefs.data.first()[longPreferencesKey(key)] ?: 0L

    suspend fun clearPosition(key: String) {
        prefs.edit { it.remove(longPreferencesKey(key)) }
    }
}
