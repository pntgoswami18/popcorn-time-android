package com.popcorntime.android.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RemoteControlTokenStore constructor(
    @Named("remoteDataStore") private val dataStore: DataStore<Preferences>,
) {
    private val tokenKey = stringPreferencesKey("remote_control_token")

    suspend fun getOrCreateToken(): String {
        val existing = dataStore.data.first()[tokenKey]
        if (!existing.isNullOrBlank()) return existing
        val newToken = UUID.randomUUID().toString()
        dataStore.edit { it[tokenKey] = newToken }
        return newToken
    }

    suspend fun regenerateToken(): String {
        val newToken = UUID.randomUUID().toString()
        dataStore.edit { it[tokenKey] = newToken }
        return newToken
    }

    fun observeToken(): Flow<String> =
        dataStore.data.map { it[tokenKey] ?: "" }
}
