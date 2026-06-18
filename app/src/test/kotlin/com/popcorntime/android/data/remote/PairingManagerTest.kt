package com.popcorntime.android.data.remote

import com.popcorntime.android.data.remote.PairingManager.PairingResult
import com.popcorntime.android.data.remote.PairingManager.PollResult
import com.popcorntime.android.data.remote.PairingManager.SubmitResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingManagerTest {

    private var now = 0L
    private var issuedTokens = 0
    private val issuedForNames = mutableListOf<String>()
    private val revokedTokens = mutableListOf<String>()
    private fun newManager(ttlMs: Long = PairingManager.DEFAULT_TTL_MS) = PairingManager(
        ttlMs = ttlMs,
        clock = { now },
        issueToken = { name ->
            issuedForNames += name
            "session-token-${++issuedTokens}"
        },
        revokeToken = { revokedTokens += it },
    )

    private fun PairingManager.accept(code: String, ip: String = "10.0.0.2"): String {
        val result = submitCode(code, "Test device", ip)
        assertTrue("expected Accepted, was $result", result is SubmitResult.Accepted)
        return (result as SubmitResult.Accepted).pairingId
    }

    @Test
    fun `pairing code is six digits`() {
        val manager = newManager()
        val code = manager.startPairing()
        assertTrue("code '$code' should be 6 digits", code.matches(Regex("\\d{6}")))
        assertEquals(code, manager.uiState.value.code)
        assertEquals(90, manager.uiState.value.secondsLeft)
    }

    @Test
    fun `code expires after ttl`() {
        val manager = newManager(ttlMs = 60_000)
        val code = manager.startPairing()
        now = 60_001
        assertEquals(SubmitResult.Expired, manager.submitCode(code, null, "ip"))
        assertNull(manager.uiState.value.code)
        assertEquals(PairingResult.EXPIRED, manager.uiState.value.lastResult)
    }

    @Test
    fun `tick publishes countdown and expiry`() {
        val manager = newManager(ttlMs = 90_000)
        manager.startPairing()
        now = 30_000
        manager.tick()
        assertEquals(60, manager.uiState.value.secondsLeft)
        now = 90_001
        manager.tick()
        assertNull(manager.uiState.value.code)
        assertEquals(PairingResult.EXPIRED, manager.uiState.value.lastResult)
    }

    @Test
    fun `five wrong attempts kill the session`() {
        val manager = newManager()
        val code = manager.startPairing()
        val wrong = if (code == "000000") "111111" else "000000"
        repeat(5) { i ->
            // Use distinct IPs so the per-IP rate limit doesn't interfere.
            assertEquals(SubmitResult.InvalidCode, manager.submitCode(wrong, null, "ip$i"))
        }
        // Session is gone: even the right code is now rejected.
        assertEquals(SubmitResult.Expired, manager.submitCode(code, null, "ip9"))
        assertEquals(PairingResult.EXPIRED, manager.uiState.value.lastResult)
    }

    @Test
    fun `per ip rate limit caps attempts in window`() {
        val manager = newManager()
        repeat(10) {
            // No active session, so these all return Expired — but still count.
            assertEquals(SubmitResult.Expired, manager.submitCode("123456", null, "1.2.3.4"))
        }
        assertEquals(SubmitResult.RateLimited, manager.submitCode("123456", null, "1.2.3.4"))
        // Other IPs are unaffected.
        assertEquals(SubmitResult.Expired, manager.submitCode("123456", null, "5.6.7.8"))
        // The window slides: a minute later the IP may try again.
        now += 61_000
        assertEquals(SubmitResult.Expired, manager.submitCode("123456", null, "1.2.3.4"))
    }

    @Test
    fun `happy path issues token exactly once`() = runBlocking {
        val manager = newManager()
        val code = manager.startPairing()
        val pairingId = manager.accept(code)
        assertEquals("Test device", manager.uiState.value.confirmationRequest?.clientName)
        assertEquals("10.0.0.2", manager.uiState.value.confirmationRequest?.clientIp)
        assertEquals(PollResult.Pending, manager.pollStatus(pairingId))
        manager.confirm()
        assertEquals(PairingResult.PAIRED, manager.uiState.value.lastResult)
        assertNull(manager.uiState.value.code)
        val poll = manager.pollStatus(pairingId)
        assertTrue("expected Confirmed, was $poll", poll is PollResult.Confirmed)
        assertEquals("session-token-1", (poll as PollResult.Confirmed).token)
        // Second pickup must fail.
        assertEquals(PollResult.Gone, manager.pollStatus(pairingId))
    }

    @Test
    fun `deny reports 403 to client and result to ui`() {
        val manager = newManager()
        val code = manager.startPairing()
        val pairingId = manager.accept(code)
        manager.deny()
        assertEquals(PollResult.Denied, manager.pollStatus(pairingId))
        assertEquals(PairingResult.DENIED, manager.uiState.value.lastResult)
        assertNull(manager.uiState.value.code)
    }

    @Test
    fun `second client gets already pending`() {
        val manager = newManager()
        val code = manager.startPairing()
        manager.accept(code)
        assertEquals(SubmitResult.AlreadyPending, manager.submitCode(code, "Other", "10.0.0.3"))
    }

    @Test
    fun `unknown pairing id is gone`() {
        val manager = newManager()
        val code = manager.startPairing()
        manager.accept(code)
        assertEquals(PollResult.Gone, manager.pollStatus("not-a-real-id"))
    }

    @Test
    fun `awaiting confirmation expires like an active code`() {
        val manager = newManager(ttlMs = 90_000)
        val code = manager.startPairing()
        val pairingId = manager.accept(code)
        now = 90_001
        assertEquals(PollResult.Gone, manager.pollStatus(pairingId))
        assertEquals(PairingResult.EXPIRED, manager.uiState.value.lastResult)
    }

    @Test
    fun `confirmed token survives past code ttl within pickup grace`() = runBlocking {
        val manager = newManager(ttlMs = 90_000)
        val code = manager.startPairing()
        now = 89_000
        val pairingId = manager.accept(code)
        manager.confirm()
        now = 95_000 // past code TTL but within the 30s pickup grace
        val poll = manager.pollStatus(pairingId)
        assertTrue("expected Confirmed, was $poll", poll is PollResult.Confirmed)
    }

    @Test
    fun `new session cancels the old one`() {
        val manager = newManager()
        val code1 = manager.startPairing()
        val pairingId1 = manager.accept(code1)
        val code2 = manager.startPairing()
        assertEquals(PollResult.Gone, manager.pollStatus(pairingId1))
        assertEquals(code2, manager.uiState.value.code)
        assertNull(manager.uiState.value.confirmationRequest)
    }

    @Test
    fun `cancel clears state without a result`() {
        val manager = newManager()
        manager.startPairing()
        manager.cancelPairing()
        assertNull(manager.uiState.value.code)
        assertNull(manager.uiState.value.lastResult)
        assertEquals(SubmitResult.Expired, manager.submitCode("123456", null, "ip"))
    }

    @Test
    fun `confirm without pending request is a no-op`() = runBlocking {
        val manager = newManager()
        manager.startPairing()
        manager.confirm() // nothing awaiting confirmation
        assertEquals(0, issuedTokens)
        assertNotEquals(PairingResult.PAIRED, manager.uiState.value.lastResult)
    }

    @Test
    fun `client name is passed through to the token issuer`() = runBlocking {
        val manager = newManager()
        val code = manager.startPairing()
        manager.accept(code)
        manager.confirm()
        assertEquals(listOf("Test device"), issuedForNames)
        assertTrue(revokedTokens.isEmpty())
    }

    @Test
    fun `token issued while session is cancelled mid-flight is revoked, not staged`() = runBlocking {
        // issueToken runs outside the lock; simulate the session being cancelled
        // exactly during issuance (e.g. user backs out / server stops).
        lateinit var manager: PairingManager
        manager = PairingManager(
            clock = { now },
            issueToken = {
                manager.cancelPairing()
                "orphan-token"
            },
            revokeToken = { revokedTokens += it },
        )
        val code = manager.startPairing()
        val pairingId = manager.accept(code)
        manager.confirm()
        assertEquals(listOf("orphan-token"), revokedTokens)
        assertEquals(PollResult.Gone, manager.pollStatus(pairingId))
        assertNotEquals(PairingResult.PAIRED, manager.uiState.value.lastResult)
    }

    @Test
    fun `confirm landing at ttl expiry revokes the token`() = runBlocking {
        lateinit var manager: PairingManager
        manager = PairingManager(
            ttlMs = 90_000,
            clock = { now },
            issueToken = {
                now = 90_001 // session expires while the token is being issued
                "late-token"
            },
            revokeToken = { revokedTokens += it },
        )
        val code = manager.startPairing()
        val pairingId = manager.accept(code)
        manager.confirm()
        assertEquals(listOf("late-token"), revokedTokens)
        assertEquals(PollResult.Gone, manager.pollStatus(pairingId))
        assertNotEquals(PairingResult.PAIRED, manager.uiState.value.lastResult)
    }

    @Test
    fun `rate limit precheck reflects state without recording attempts`() {
        val manager = newManager()
        // Prechecks never count as attempts.
        repeat(50) { assertFalse(manager.isRateLimitedPrecheck("1.2.3.4")) }
        repeat(10) { assertEquals(SubmitResult.Expired, manager.submitCode("123456", null, "1.2.3.4")) }
        assertTrue(manager.isRateLimitedPrecheck("1.2.3.4"))
        assertFalse(manager.isRateLimitedPrecheck("5.6.7.8"))
        // The window slides for the precheck too.
        now += 61_000
        assertFalse(manager.isRateLimitedPrecheck("1.2.3.4"))
    }

    @Test
    fun `rate limiter map is capped and evicts the stalest ips`() {
        val manager = newManager()
        // Rate-limit one IP with attempts at t=0..9.
        repeat(10) {
            manager.submitCode("123456", null, "attacker")
            now += 1
        }
        assertEquals(SubmitResult.RateLimited, manager.submitCode("123456", null, "attacker"))
        // Flood with >100 distinct fresh IPs (all within the window) — the cap
        // must evict the attacker's (stalest) entry instead of growing forever.
        repeat(110) { i ->
            manager.submitCode("123456", null, "10.0.0.$i")
            now += 1
        }
        // The attacker's history was evicted, so it is no longer rate limited.
        assertEquals(SubmitResult.Expired, manager.submitCode("123456", null, "attacker"))
    }
}
