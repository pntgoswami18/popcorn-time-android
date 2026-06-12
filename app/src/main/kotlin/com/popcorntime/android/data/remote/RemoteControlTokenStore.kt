package com.popcorntime.android.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

    // --- Enabled toggle --------------------------------------------------------

    private val enabledKey = booleanPreferencesKey("remote_control_enabled")

    /**
     * Whether the user has enabled remote control. This is the single source of
     * truth for whether the server should be running — defaults to false so the
     * API is never exposed without an explicit opt-in.
     */
    fun observeEnabled(): Flow<Boolean> =
        dataStore.data.map { it[enabledKey] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[enabledKey] = enabled }
    }

    // --- Session tokens (issued via the QR pairing flow) ---------------------

    private val sessionTokensKey = stringSetPreferencesKey("remote_session_tokens")

    /** Issues and persists a new session token for a freshly paired device. */
    suspend fun issueSessionToken(): String = mutex.withLock {
        val token = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            prefs[sessionTokensKey] = (prefs[sessionTokensKey] ?: emptySet()) + token
        }
        token
    }

    fun observeSessionTokens(): Flow<Set<String>> =
        dataStore.data.map { it[sessionTokensKey] ?: emptySet() }

    /** Revokes every paired device at once. The persistent token is unaffected. */
    suspend fun revokeAllSessionTokens() = mutex.withLock {
        dataStore.edit { it.remove(sessionTokensKey) }
    }
}
