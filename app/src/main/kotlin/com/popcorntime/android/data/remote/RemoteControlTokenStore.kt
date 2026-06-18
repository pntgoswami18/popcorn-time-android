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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** Metadata for one session token issued to a paired device. */
@Serializable
data class SessionTokenInfo(
    val token: String,
    val name: String,
    val issuedAt: Long,
)

/**
 * Merges the JSON-encoded token list with any tokens still stored under the
 * legacy string-set key (migrated lazily with name "Paired device" / issuedAt 0).
 * Top-level and pure so the migration logic is JVM unit-testable.
 */
internal fun mergeSessionTokenInfos(rawJson: String?, legacyTokens: Set<String>?): List<SessionTokenInfo> {
    val json = Json { ignoreUnknownKeys = true }
    val current = rawJson
        ?.let { runCatching { json.decodeFromString<List<SessionTokenInfo>>(it) }.getOrNull() }
        ?: emptyList()
    val known = current.mapTo(mutableSetOf()) { it.token }
    val migrated = legacyTokens.orEmpty()
        .filter { it.isNotBlank() && it !in known }
        .map { SessionTokenInfo(token = it, name = "Paired device", issuedAt = 0L) }
    return current + migrated
}

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
    //
    // Stored as a JSON list of SessionTokenInfo so each paired device can be
    // listed and revoked individually. Tokens persisted under the old
    // string-set key are migrated on read (and the old key cleared on the next
    // write) with the placeholder name "Paired device".

    private val sessionTokenInfosKey = stringPreferencesKey("remote_session_token_infos")
    private val legacySessionTokensKey = stringSetPreferencesKey("remote_session_tokens")
    private val json = Json { ignoreUnknownKeys = true }

    /** Issues and persists a new session token for a freshly paired device. */
    suspend fun issueSessionToken(name: String = "Paired device"): String = mutex.withLock {
        val token = UUID.randomUUID().toString()
        val info = SessionTokenInfo(
            token = token,
            name = name.take(64).ifBlank { "Paired device" },
            issuedAt = System.currentTimeMillis(),
        )
        editSessionTokenInfos { infos -> infos + info }
        token
    }

    /** Token values only — what the server compares bearer tokens against. */
    fun observeSessionTokens(): Flow<Set<String>> =
        observeSessionTokenInfos().map { infos -> infos.mapTo(mutableSetOf()) { it.token } }

    /** Full per-device metadata, for the paired-devices list in settings. */
    fun observeSessionTokenInfos(): Flow<List<SessionTokenInfo>> =
        dataStore.data.map { prefs ->
            mergeSessionTokenInfos(prefs[sessionTokenInfosKey], prefs[legacySessionTokensKey])
        }

    /** Revokes a single paired device's token. */
    suspend fun revokeSessionToken(token: String) = mutex.withLock {
        editSessionTokenInfos { infos -> infos.filterNot { it.token == token } }
    }

    /** Revokes every paired device at once. The persistent token is unaffected. */
    suspend fun revokeAllSessionTokens() = mutex.withLock {
        dataStore.edit {
            it.remove(sessionTokenInfosKey)
            it.remove(legacySessionTokensKey)
        }
    }

    /** Applies [transform] to the merged (current + legacy) list and persists the result,
     *  completing the migration off the legacy key. Call with [mutex] held. */
    private suspend fun editSessionTokenInfos(transform: (List<SessionTokenInfo>) -> List<SessionTokenInfo>) {
        dataStore.edit { prefs ->
            val merged = mergeSessionTokenInfos(prefs[sessionTokenInfosKey], prefs[legacySessionTokensKey])
            prefs[sessionTokenInfosKey] = json.encodeToString(transform(merged))
            prefs.remove(legacySessionTokensKey)
        }
    }
}
