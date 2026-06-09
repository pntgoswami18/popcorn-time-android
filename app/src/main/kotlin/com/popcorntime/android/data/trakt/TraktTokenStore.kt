package com.popcorntime.android.data.trakt

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Singleton
class TraktTokenStore constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val accessTokenKey = stringPreferencesKey("trakt_access_token")
    private val refreshTokenKey = stringPreferencesKey("trakt_refresh_token")
    private val expiresAtKey = longPreferencesKey("trakt_expires_at")
    private val favouritesSlugKey = stringPreferencesKey("trakt_favourites_slug")

    fun observeAccessToken(): Flow<String?> =
        dataStore.data.map { it[accessTokenKey] }

    fun isTraktConnected(): Flow<Boolean> =
        dataStore.data.map { prefs ->
            val token = prefs[accessTokenKey]
            val expiresAt = prefs[expiresAtKey] ?: 0L
            !token.isNullOrBlank() && System.currentTimeMillis() < expiresAt
        }

    suspend fun getAccessToken(): String? {
        val prefs = dataStore.data.first()
        val token = prefs[accessTokenKey] ?: return null
        val expiresAt = prefs[expiresAtKey] ?: 0L
        return if (System.currentTimeMillis() < expiresAt) token else null
    }

    suspend fun isLoggedIn(): Boolean =
        isTraktConnected().first()

    suspend fun saveToken(accessToken: String, refreshToken: String, expiresIn: Long, createdAt: Long) {
        dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
            prefs[expiresAtKey] = createdAt * 1000L + expiresIn * 1000L
        }
    }

    suspend fun clearToken() {
        dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
            prefs.remove(expiresAtKey)
        }
    }

    suspend fun getFavouritesSlug(): String? =
        dataStore.data.first()[favouritesSlugKey]

    suspend fun saveFavouritesSlug(slug: String) {
        dataStore.edit { it[favouritesSlugKey] = slug }
    }
}
