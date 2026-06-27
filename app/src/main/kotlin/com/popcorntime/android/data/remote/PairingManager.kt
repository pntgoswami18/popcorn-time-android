package com.popcorntime.android.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.UUID

/**
 * In-memory manager for the short-lived QR pairing flow.
 *
 * At most one pairing session is active at a time. The flow is:
 *
 * 1. The user taps "Pair a new device" → [startPairing] generates a random
 *    6-digit code with a TTL of [ttlMs] (90s by default, configurable within
 *    the 60–120s window). The QR encodes only this code — never a token.
 * 2. The remote web client scans the QR and calls [submitCode]. On a match the
 *    session moves to AwaitingConfirmation and the phone shows a dialog.
 * 3. The client polls [pollStatus] while the user decides. On [confirm] a
 *    fresh session token is issued via [issueToken] and staged for exactly one
 *    pickup by the polling client; on [deny] the client gets 403.
 *
 * No internal coroutines: time is injected via [clock] (elapsed-realtime-like,
 * monotonic, in ms) and expiry is evaluated lazily at every entry point plus
 * whenever the owning ViewModel calls [tick] from its countdown loop. This
 * keeps the class trivially testable on the JVM.
 *
 * Thread safety: all state lives behind [lock]; methods are called from
 * NanoHTTPD worker threads and from ViewModel coroutines.
 */
class PairingManager(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val clock: () -> Long,
    /** Issues and persists a fresh session token for the given client name. */
    private val issueToken: suspend (clientName: String) -> String,
    /** Revokes a persisted token that could not be staged (session died mid-issuance). */
    private val revokeToken: suspend (token: String) -> Unit = {},
) {
    companion object {
        /** Pairing code TTL. Must stay within the 60–120s acceptance window. */
        const val DEFAULT_TTL_MS = 90_000L

        /** Extra time granted after confirmation for the client to pick up the token. */
        private const val TOKEN_PICKUP_GRACE_MS = 30_000L

        /** Wrong-code attempts allowed before the session self-destructs. */
        private const val MAX_FAILED_ATTEMPTS = 5

        /** Per-IP sliding window rate limit for POST /pair. */
        private const val IP_WINDOW_MS = 60_000L
        private const val IP_MAX_ATTEMPTS = 10

        /** Cap on distinct IPs tracked by the rate limiter (oldest evicted beyond this). */
        private const val MAX_TRACKED_IPS = 100

        private const val CODE_LENGTH = 6
    }

    enum class PairingResult { PAIRED, DENIED, EXPIRED }

    data class ConfirmationRequest(val clientName: String, val clientIp: String)

    data class PairingUiState(
        /** Active pairing code to show in the QR / as manual fallback, or null when idle. */
        val code: String? = null,
        val secondsLeft: Int = 0,
        /** Non-null while a client waits for the user to Allow/Deny. */
        val confirmationRequest: ConfirmationRequest? = null,
        /** Terminal outcome of the last session, until cleared or a new session starts. */
        val lastResult: PairingResult? = null,
    )

    sealed interface SubmitResult {
        data class Accepted(val pairingId: String) : SubmitResult
        data object InvalidCode : SubmitResult
        data object Expired : SubmitResult
        data object RateLimited : SubmitResult
        data object AlreadyPending : SubmitResult
    }

    sealed interface PollResult {
        data object Pending : PollResult
        data class Confirmed(val token: String) : PollResult
        data object Denied : PollResult
        data object Gone : PollResult
    }

    private enum class State { CODE_ACTIVE, AWAITING_CONFIRMATION, CONFIRMED, DENIED }

    private class Session(
        val code: String,
        var expiresAtMs: Long,
        var state: State = State.CODE_ACTIVE,
        var pairingId: String? = null,
        var clientName: String? = null,
        var clientIp: String? = null,
        var failedAttempts: Int = 0,
        var stagedToken: String? = null,
    )

    private val lock = Any()
    private val random = SecureRandom()
    private var session: Session? = null
    private val ipAttempts = HashMap<String, ArrayDeque<Long>>()

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    /** Starts a new pairing session, cancelling any previous one. Returns the new code. */
    fun startPairing(): String = synchronized(lock) {
        val code = generateCode()
        session = Session(code = code, expiresAtMs = clock() + ttlMs)
        publishState()
        code
    }

    /** Cancels the active session, if any, without recording a terminal result. */
    fun cancelPairing() = synchronized(lock) {
        session = null
        _uiState.value = PairingUiState()
    }

    /** Clears the last terminal result (after the UI has shown it). */
    fun clearResult() = synchronized(lock) {
        _uiState.value = _uiState.value.copy(lastResult = null)
    }

    /**
     * Re-evaluates expiry and refreshes [uiState] (e.g. the countdown).
     * Intended to be called ~once per second by the ViewModel while a session
     * is active.
     */
    fun tick() = synchronized(lock) {
        expireIfNeeded()
        publishState()
    }

    /** Handles a pairing code submitted by a remote client (POST /pair). */
    fun submitCode(code: String, clientName: String?, clientIp: String): SubmitResult = synchronized(lock) {
        if (isRateLimited(clientIp)) return SubmitResult.RateLimited
        expireIfNeeded()
        val s = session ?: return SubmitResult.Expired
        if (s.state != State.CODE_ACTIVE) return SubmitResult.AlreadyPending
        if (!constantTimeEquals(code, s.code)) {
            s.failedAttempts++
            if (s.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // Too many guesses: kill the session so the code can't be brute-forced.
                session = null
                _uiState.value = PairingUiState(lastResult = PairingResult.EXPIRED)
            }
            return SubmitResult.InvalidCode
        }
        val pairingId = UUID.randomUUID().toString()
        s.state = State.AWAITING_CONFIRMATION
        s.pairingId = pairingId
        s.clientName = clientName?.take(64)?.ifBlank { null }
        s.clientIp = clientIp
        publishState()
        SubmitResult.Accepted(pairingId)
    }

    /** Polled by the remote client while waiting for on-device confirmation. */
    fun pollStatus(pairingId: String): PollResult = synchronized(lock) {
        expireIfNeeded()
        val s = session ?: return PollResult.Gone
        // Constant-time: pairingId is an unauthenticated client-supplied secret.
        if (!constantTimeEquals(pairingId, s.pairingId ?: return PollResult.Gone)) return PollResult.Gone
        when (s.state) {
            State.AWAITING_CONFIRMATION -> PollResult.Pending
            State.DENIED -> PollResult.Denied
            State.CONFIRMED -> {
                val token = s.stagedToken
                if (token != null) {
                    // One-time pickup: hand out the token and end the session.
                    s.stagedToken = null
                    session = null
                    PollResult.Confirmed(token)
                } else {
                    PollResult.Gone
                }
            }
            State.CODE_ACTIVE -> PollResult.Gone
        }
    }

    /**
     * Called when the user taps Allow on the confirmation dialog. Issues a
     * fresh session token and stages it for one-time pickup by [pollStatus].
     *
     * Issuance (which persists the token) happens outside the lock; if the
     * session died in the meantime (cancelled, replaced, or expired) the token
     * is revoked again so no orphaned, forever-valid token survives.
     */
    suspend fun confirm() {
        // Pre-check without holding the lock across the suspend point.
        // Capture the pairingId so we can verify it's still the *same* session
        // after the suspend; a concurrent startPairing() could replace session.
        val (clientName, capturedPairingId) = synchronized(lock) {
            expireIfNeeded()
            val s = session
            if (s == null || s.state != State.AWAITING_CONFIRMATION) return
            Pair(s.clientName ?: "Paired device", s.pairingId)
        }
        val token = issueToken(clientName)
        val staged = synchronized(lock) {
            expireIfNeeded()
            val s = session
            if (s != null && s.state == State.AWAITING_CONFIRMATION &&
                s.pairingId == capturedPairingId) {
                s.state = State.CONFIRMED
                s.stagedToken = token
                s.expiresAtMs = maxOf(s.expiresAtMs, clock() + TOKEN_PICKUP_GRACE_MS)
                _uiState.value = PairingUiState(lastResult = PairingResult.PAIRED)
                true
            } else {
                false
            }
        }
        if (!staged) revokeToken(token)
    }

    /** Called when the user taps Deny on the confirmation dialog. */
    fun deny() = synchronized(lock) {
        val s = session ?: return
        if (s.state != State.AWAITING_CONFIRMATION) return
        s.state = State.DENIED
        _uiState.value = PairingUiState(lastResult = PairingResult.DENIED)
    }

    // --- internals (call with lock held) ------------------------------------

    private fun expireIfNeeded() {
        val s = session ?: return
        if (clock() <= s.expiresAtMs) return
        session = null
        when (s.state) {
            State.CODE_ACTIVE, State.AWAITING_CONFIRMATION ->
                _uiState.value = PairingUiState(lastResult = PairingResult.EXPIRED)
            // Terminal states already reported their result; just drop the session.
            State.CONFIRMED, State.DENIED ->
                _uiState.value = _uiState.value.copy(code = null, secondsLeft = 0, confirmationRequest = null)
        }
    }

    private fun publishState() {
        val s = session
        if (s == null || s.state == State.CONFIRMED || s.state == State.DENIED) return
        val secondsLeft = ((s.expiresAtMs - clock()).coerceAtLeast(0L) / 1000L).toInt()
        _uiState.value = PairingUiState(
            code = s.code,
            secondsLeft = secondsLeft,
            confirmationRequest = if (s.state == State.AWAITING_CONFIRMATION) {
                ConfirmationRequest(
                    clientName = s.clientName ?: "Unknown device",
                    clientIp = s.clientIp ?: "unknown",
                )
            } else {
                null
            },
            lastResult = null,
        )
    }

    private fun isRateLimited(clientIp: String): Boolean {
        val now = clock()
        pruneIpAttempts(now)
        val existing = ipAttempts[clientIp]
        if (existing != null && existing.size >= IP_MAX_ATTEMPTS) return true
        val attempts = existing ?: ArrayDeque<Long>().also { ipAttempts[clientIp] = it }
        attempts.addLast(now)
        evictOverflowingIps()
        return false
    }

    /**
     * Read-only rate-limit check for the server's pre-parse guard on POST /pair:
     * lets the request be rejected BEFORE its body is read. Records nothing —
     * [submitCode] still does the authoritative check-and-record.
     */
    fun isRateLimitedPrecheck(clientIp: String): Boolean = synchronized(lock) {
        pruneIpAttempts(clock())
        (ipAttempts[clientIp]?.size ?: 0) >= IP_MAX_ATTEMPTS
    }

    /** Drops attempts older than the window and removes IPs whose deque is empty. */
    private fun pruneIpAttempts(now: Long) {
        val iterator = ipAttempts.entries.iterator()
        while (iterator.hasNext()) {
            val (_, attempts) = iterator.next()
            while (attempts.isNotEmpty() && now - attempts.first() > IP_WINDOW_MS) {
                attempts.removeFirst()
            }
            if (attempts.isEmpty()) iterator.remove()
        }
    }

    /** Caps the tracked-IP map; evicts the IPs with the oldest most-recent attempt. */
    private fun evictOverflowingIps() {
        while (ipAttempts.size > MAX_TRACKED_IPS) {
            val oldest = ipAttempts.entries.minByOrNull { it.value.lastOrNull() ?: Long.MIN_VALUE }
                ?: return
            ipAttempts.remove(oldest.key)
        }
    }

    private fun generateCode(): String =
        buildString(CODE_LENGTH) { repeat(CODE_LENGTH) { append(random.nextInt(10)) } }
}
