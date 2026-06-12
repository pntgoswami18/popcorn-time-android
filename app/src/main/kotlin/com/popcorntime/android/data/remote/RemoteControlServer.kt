package com.popcorntime.android.data.remote

import android.content.Context
import com.popcorntime.android.data.torrent.TorrentEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteControlServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackController: PlaybackController,
    private val playbackQueue: PlaybackQueue,
    private val torrentEngine: TorrentEngine,
    private val tokenStore: RemoteControlTokenStore,
    private val pairingManager: PairingManager,
    private val tlsCertificateManager: RemoteTlsCertificateManager,
) : NanoHTTPD(8889) {

    companion object {
        const val REMOTE_PORT = 8889
        private const val MIME_JSON = "application/json"
        private const val MIME_HTML_UTF8 = "text/html; charset=utf-8"

        // NanoHTTPD 2.3.1's Response.Status enum lacks some of the codes the
        // pairing protocol uses (410, 429), so build them explicitly.
        private fun httpStatus(code: Int, description: String): Response.IStatus =
            object : Response.IStatus {
                override fun getRequestStatus(): Int = code
                override fun getDescription(): String = "$code $description"
            }

        private val STATUS_ACCEPTED = httpStatus(202, "Accepted")
        private val STATUS_CONFLICT = httpStatus(409, "Conflict")
        private val STATUS_GONE = httpStatus(410, "Gone")
        private val STATUS_TOO_MANY_REQUESTS = httpStatus(429, "Too Many Requests")
    }

    private val json = Json { ignoreUnknownKeys = true }

    // The remote control web page, bundled as an app asset. Loaded once on
    // first request and cached for the lifetime of the process.
    private val indexHtml: String by lazy {
        context.assets.open("remote/index.html").bufferedReader().use { it.readText() }
    }

    private val cachedTokenRef = AtomicReference<String?>(null)

    // Session tokens issued through the QR pairing flow. Kept in sync with the
    // token store by a collector that runs while the server is alive. The
    // server is started/stopped exclusively by RemoteControlServerController
    // in response to the user's persisted remote-control toggle.
    private val sessionTokensRef = AtomicReference<Set<String>>(emptySet())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionTokenJob: Job? = null

    private val _isAlive = MutableStateFlow(false)
    val isAliveFlow: StateFlow<Boolean> = _isAlive.asStateFlow()

    // Keystore lookup / key generation and SSL context setup are blocking
    // I/O, so the whole start path runs on Dispatchers.IO.
    suspend fun startIfNotRunning(token: String) = withContext(Dispatchers.IO) {
        if (!isAlive) {
            cachedTokenRef.set(token)
            sessionTokenJob?.cancel()
            sessionTokenJob = scope.launch {
                tokenStore.observeSessionTokens().collect { sessionTokensRef.set(it) }
            }
            // TLS only — never serve the API over cleartext HTTP. makeSecure
            // just records the socket factory, so calling it before every
            // start() keeps restart cycles secure too.
            makeSecure(
                tlsCertificateManager.createServerSocketFactory(),
                tlsCertificateManager.enabledTlsProtocols().takeIf { it.isNotEmpty() },
            )
            start(SOCKET_READ_TIMEOUT, false)
            _isAlive.value = isAlive
            Timber.d("RemoteControlServer started on port $REMOTE_PORT (TLS)")
        }
    }

    fun stopIfRunning() {
        if (isAlive) {
            stop()
            sessionTokenJob?.cancel()
            sessionTokenJob = null
            pairingManager.cancelPairing()
            _isAlive.value = false
            Timber.d("RemoteControlServer stopped")
        }
    }

    fun invalidateToken() { cachedTokenRef.set(null) }

    fun updateToken(token: String) {
        cachedTokenRef.set(token)
    }

    override fun serve(session: IHTTPSession): Response {
        // Unauthenticated routes: the remote control web page itself. The page
        // bootstraps its token from the URL fragment (which never reaches the
        // server), so it must be reachable without a bearer token. All API
        // routes below remain protected.
        if (session.method == Method.GET && (session.uri == "/" || session.uri == "/index.html")) {
            return serveIndexPage()
        }
        if (session.uri == "/favicon.ico") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_JSON, """{"error":"Not Found"}""")
        }

        // Pairing endpoints are pre-auth by design: the client has no token
        // yet. They are guarded by the short-lived code, on-device
        // confirmation, and per-IP rate limiting inside PairingManager.
        if (session.method == Method.POST && session.uri == "/pair") {
            return handlePairStart(session)
        }
        if (session.method == Method.GET && session.uri == "/pair/status") {
            return handlePairStatus(session)
        }

        // Bearer token auth: the persistent token (Advanced/manual flow) or
        // any session token issued through QR pairing.
        val authHeader = session.headers["authorization"] ?: ""
        val expectedToken = cachedTokenRef.get()
            ?: return newFixedLengthResponse(
                Response.Status.SERVICE_UNAVAILABLE,
                MIME_JSON,
                """{"error":"Server not ready"}""",
            )
        val bearer = authHeader.removePrefix("Bearer ").trim()
        val authorized = constantTimeEquals(bearer, expectedToken) ||
            sessionTokensRef.get().any { constantTimeEquals(bearer, it) }
        if (!authorized) {
            return newFixedLengthResponse(
                Response.Status.UNAUTHORIZED,
                MIME_JSON,
                """{"error":"Unauthorized"}""",
            )
        }

        val method = session.method
        val uri = session.uri

        return when {
            method == Method.GET && uri == "/status" -> handleStatus()
            method == Method.POST && uri == "/play" -> handlePlay()
            method == Method.POST && uri == "/pause" -> handlePause()
            method == Method.POST && uri == "/seek" -> handleSeek(session)
            method == Method.GET && uri == "/queue" -> handleQueueGet()
            method == Method.POST && uri == "/queue/add" -> handleQueueAdd(session)
            method == Method.DELETE && uri == "/queue/clear" -> handleQueueClear()
            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_JSON,
                """{"error":"Not Found"}""",
            )
        }
    }

    // --- Pairing -------------------------------------------------------------

    private fun handlePairStart(session: IHTTPSession): Response {
        val request = try {
            val bodyMap = mutableMapOf<String, String>()
            session.parseBody(bodyMap)
            val body = bodyMap["postData"] ?: bodyMap.values.firstOrNull() ?: ""
            json.decodeFromString<PairRequest>(body)
        } catch (e: Exception) {
            return pairError(Response.Status.BAD_REQUEST, "invalid_request")
        }
        val clientIp = session.remoteIpAddress ?: "unknown"
        return when (val result = pairingManager.submitCode(request.code, request.clientName, clientIp)) {
            is PairingManager.SubmitResult.Accepted -> newFixedLengthResponse(
                STATUS_ACCEPTED,
                MIME_JSON,
                json.encodeToString(PairStartResponse(pairingId = result.pairingId)),
            )
            PairingManager.SubmitResult.InvalidCode ->
                pairError(Response.Status.UNAUTHORIZED, "invalid_code")
            PairingManager.SubmitResult.Expired ->
                pairError(STATUS_GONE, "expired")
            PairingManager.SubmitResult.AlreadyPending ->
                pairError(STATUS_CONFLICT, "already_pending")
            PairingManager.SubmitResult.RateLimited ->
                pairError(STATUS_TOO_MANY_REQUESTS, "rate_limited")
        }
    }

    private fun handlePairStatus(session: IHTTPSession): Response {
        val pairingId = session.parms["pairingId"]
            ?: return pairError(Response.Status.BAD_REQUEST, "missing_pairing_id")
        return when (val result = pairingManager.pollStatus(pairingId)) {
            PairingManager.PollResult.Pending -> newFixedLengthResponse(
                Response.Status.OK,
                MIME_JSON,
                json.encodeToString(PairStatusResponse(status = "pending")),
            )
            is PairingManager.PollResult.Confirmed -> newFixedLengthResponse(
                Response.Status.OK,
                MIME_JSON,
                json.encodeToString(PairStatusResponse(status = "confirmed", token = result.token)),
            )
            PairingManager.PollResult.Denied ->
                pairError(Response.Status.FORBIDDEN, "denied")
            PairingManager.PollResult.Gone ->
                pairError(STATUS_GONE, "gone")
        }
    }

    private fun pairError(status: Response.IStatus, error: String): Response =
        newFixedLengthResponse(status, MIME_JSON, json.encodeToString(PairErrorResponse(error)))

    private fun serveIndexPage(): Response {
        return try {
            newFixedLengthResponse(Response.Status.OK, MIME_HTML_UTF8, indexHtml)
        } catch (e: Exception) {
            Timber.e(e, "RemoteControlServer: failed to load remote control page asset")
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_JSON,
                """{"error":"Remote control page unavailable"}""",
            )
        }
    }

    private fun handleStatus(): Response {
        val isPlaying = playbackController.isPlaying.value
        val positionMs = playbackController.playerPositionMs.value
        val durationMs = playbackController.playerDurationMs.value
        val streamState = torrentEngine.state.value::class.simpleName ?: "Unknown"
        val queue = Json.encodeToString(playbackQueue.items.value)
        val encodedState = Json.encodeToString(streamState)
        val json = """{"isPlaying":$isPlaying,"positionMs":$positionMs,"durationMs":$durationMs,"streamState":$encodedState,"queue":$queue}"""
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, json)
    }

    private fun handlePlay(): Response {
        playbackController.command.tryEmit(PlaybackCommand.Play)
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, """{"ok":true}""")
    }

    private fun handlePause(): Response {
        playbackController.command.tryEmit(PlaybackCommand.Pause)
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, """{"ok":true}""")
    }

    private fun handleSeek(session: IHTTPSession): Response {
        val positionStr = session.parms["position"]
            ?: return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                MIME_JSON,
                """{"error":"Missing position parameter"}""",
            )
        val position = positionStr.toLongOrNull()
            ?: return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                MIME_JSON,
                """{"error":"Invalid position value"}""",
            )
        playbackController.command.tryEmit(PlaybackCommand.SeekTo(position))
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, """{"ok":true}""")
    }

    private fun handleQueueGet(): Response {
        val json = Json.encodeToString(playbackQueue.items.value)
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, json)
    }

    private fun handleQueueAdd(session: IHTTPSession): Response {
        return try {
            val bodyMap = mutableMapOf<String, String>()
            session.parseBody(bodyMap)
            val body = bodyMap["postData"] ?: bodyMap.values.firstOrNull() ?: ""
            val item = Json { ignoreUnknownKeys = true }.decodeFromString<QueueItem>(body)
            playbackQueue.enqueue(item)
            newFixedLengthResponse(Response.Status.OK, MIME_JSON, """{"ok":true}""")
        } catch (e: Exception) {
            Timber.e(e, "RemoteControlServer: failed to parse queue item")
            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                MIME_JSON,
                """{"error":"Invalid request body"}""",
            )
        }
    }

    private fun handleQueueClear(): Response {
        playbackQueue.clear()
        return newFixedLengthResponse(Response.Status.OK, MIME_JSON, """{"ok":true}""")
    }
}
