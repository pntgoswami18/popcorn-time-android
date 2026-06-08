package com.popcorntime.android.data.subtitles

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Singleton
class OsTokenStore constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val tokenKey = stringPreferencesKey("os_token")
    private val usernameKey = stringPreferencesKey("os_username")
    private val baseUrlKey = stringPreferencesKey("os_base_url")
    private val preferredLanguagesKey = stringPreferencesKey("os_preferred_languages")

    fun isLoggedIn(): Flow<Boolean> =
        dataStore.data.map { prefs ->
            !prefs[tokenKey].isNullOrBlank()
        }

    suspend fun getToken(): String? =
        dataStore.data.first()[tokenKey]

    suspend fun getBaseUrl(): String? =
        dataStore.data.first()[baseUrlKey]

    suspend fun getUsername(): String? =
        dataStore.data.first()[usernameKey]

    suspend fun saveToken(username: String, token: String, baseUrl: String) {
        dataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[usernameKey] = username
            prefs[baseUrlKey] = baseUrl
        }
    }

    suspend fun clearToken() {
        dataStore.edit { prefs ->
            prefs.remove(tokenKey)
            prefs.remove(usernameKey)
            prefs.remove(baseUrlKey)
        }
    }

    suspend fun getPreferredLanguages(): List<String> {
        val raw = dataStore.data.first()[preferredLanguagesKey] ?: "en"
        return raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun savePreferredLanguages(languages: List<String>) {
        dataStore.edit { prefs ->
            prefs[preferredLanguagesKey] = languages.joinToString(",")
        }
    }
}
