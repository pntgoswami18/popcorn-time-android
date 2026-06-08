package com.popcorntime.android.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RemoteControlTokenStore @Inject constructor(
    @Named("remoteDataStore") private val dataStore: DataStore<Preferences>,
) {
    private val tokenKey = stringPreferencesKey("remote_control_token")
    private val mutex = Mutex()

    suspend fun getOrCreateToken(): String = mutex.withLock {
        val existing = dataStore.data.first()[tokenKey]
        if (!existing.isNullOrBlank()) return@withLock existing
        val newToken = UUID.randomUUID().toString()
        dataStore.edit { it[tokenKey] = newToken }
        newToken
    }

    suspend fun regenerateToken(): String = mutex.withLock {
        val newToken = UUID.randomUUID().toString()
        dataStore.edit { it[tokenKey] = newToken }
        newToken
    }

    fun observeToken(): Flow<String> =
        dataStore.data.map { it[tokenKey] ?: "" }
}
