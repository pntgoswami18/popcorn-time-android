package com.popcorntime.android.data.cast

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class KodiPrefsStore(private val dataStore: DataStore<Preferences>) {

    private val hostKey = stringPreferencesKey("kodi_host")
    private val portKey = intPreferencesKey("kodi_port")

    fun observeAddress(): Flow<Pair<String, Int>> =
        dataStore.data.map { prefs ->
            Pair(prefs[hostKey] ?: "", prefs[portKey] ?: 8080)
        }

    suspend fun saveAddress(host: String, port: Int) {
        dataStore.edit { prefs ->
            prefs[hostKey] = host
            prefs[portKey] = port
        }
    }

    suspend fun getAddress(): Pair<String, Int> = observeAddress().first()
}
