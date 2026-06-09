package com.popcorntime.android.data.remote

import com.popcorntime.android.data.torrent.TorrentEngine
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteControlServer @Inject constructor(
    private val playbackController: PlaybackController,
    private val playbackQueue: PlaybackQueue,
    private val torrentEngine: TorrentEngine,
    private val tokenStore: RemoteControlTokenStore,
) : NanoHTTPD(8889) {

    companion object {
        const val REMOTE_PORT = 8889
        private const val MIME_JSON = "application/json"
    }

    private val cachedTokenRef = AtomicReference<String?>(null)

    private val _isAlive = MutableStateFlow(false)
    val isAliveFlow: StateFlow<Boolean> = _isAlive.asStateFlow()

    fun startIfNotRunning(token: String) {
        if (!isAlive) {
            cachedTokenRef.set(token)
            start(SOCKET_READ_TIMEOUT, false)
            _isAlive.value = isAlive
            Timber.d("RemoteControlServer started on port $REMOTE_PORT")
        }
    }

    fun stopIfRunning() {
        if (isAlive) {
            stop()
            _isAlive.value = false
            Timber.d("RemoteControlServer stopped")
        }
    }

    fun invalidateToken() { cachedTokenRef.set(null) }

    override fun serve(session: IHTTPSession): Response {
        // Bearer token auth
        val authHeader = session.headers["authorization"] ?: ""
        val expectedToken = cachedTokenRef.get()
            ?: return newFixedLengthResponse(
                Response.Status.SERVICE_UNAVAILABLE,
                MIME_JSON,
                """{"error":"Server not ready"}""",
            )
        val bearer = authHeader.removePrefix("Bearer ").trim()
        if (!bearer.constantTimeEquals(expectedToken)) {
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

    private fun handleStatus(): Response {
        val isPlaying = playbackController.isPlaying.value
        val positionMs = playbackController.playerPositionMs.value
        val durationMs = playbackController.playerDurationMs.value
        val streamState = torrentEngine.state.value::class.simpleName ?: "Unknown"
        val queue = Json.encodeToString(playbackQueue.items.value)
        val json = """{"isPlaying":$isPlaying,"positionMs":$positionMs,"durationMs":$durationMs,"streamState":"$streamState","queue":$queue}"""
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

    private fun String.constantTimeEquals(other: String): Boolean {
        // Compare against the full length of `other` regardless of `this` length,
        // so that length-mismatch does not reveal information via timing.
        val len = other.length
        var diff = length xor len   // non-zero if lengths differ
        for (i in 0 until len) {
            val a = if (i < length) this[i].code else 0
            diff = diff or (a xor other[i].code)
        }
        return diff == 0
    }
}
