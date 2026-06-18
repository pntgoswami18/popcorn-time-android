package com.popcorntime.android.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the legacy string-set → per-token-metadata migration logic. */
class SessionTokenMigrationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `legacy tokens migrate with placeholder name and zero issuedAt`() {
        val merged = mergeSessionTokenInfos(rawJson = null, legacyTokens = setOf("tok-a", "tok-b"))
        assertEquals(setOf("tok-a", "tok-b"), merged.mapTo(mutableSetOf()) { it.token })
        assertTrue(merged.all { it.name == "Paired device" && it.issuedAt == 0L })
    }

    @Test
    fun `current entries win over legacy duplicates`() {
        val current = listOf(SessionTokenInfo(token = "tok-a", name = "Pixel 9", issuedAt = 123L))
        val merged = mergeSessionTokenInfos(
            rawJson = json.encodeToString(current),
            legacyTokens = setOf("tok-a", "tok-b"),
        )
        assertEquals(2, merged.size)
        val a = merged.first { it.token == "tok-a" }
        assertEquals("Pixel 9", a.name)
        assertEquals(123L, a.issuedAt)
        assertEquals("Paired device", merged.first { it.token == "tok-b" }.name)
    }

    @Test
    fun `corrupt json falls back to legacy tokens only`() {
        val merged = mergeSessionTokenInfos(rawJson = "not-json{", legacyTokens = setOf("tok-x"))
        assertEquals(listOf("tok-x"), merged.map { it.token })
    }

    @Test
    fun `blank legacy tokens are ignored`() {
        val merged = mergeSessionTokenInfos(rawJson = null, legacyTokens = setOf("", "tok-y"))
        assertEquals(listOf("tok-y"), merged.map { it.token })
    }

    @Test
    fun `empty inputs produce empty list`() {
        assertTrue(mergeSessionTokenInfos(null, null).isEmpty())
        assertTrue(mergeSessionTokenInfos("[]", emptySet()).isEmpty())
    }
}
