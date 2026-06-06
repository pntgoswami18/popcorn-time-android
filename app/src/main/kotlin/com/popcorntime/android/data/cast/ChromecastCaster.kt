package com.popcorntime.android.data.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

data class CastDeviceInfo(val name: String)

class ChromecastCaster(private val context: Context) {

    private val _activeSession = MutableStateFlow<CastSession?>(null)
    val activeSession: StateFlow<CastSession?> = _activeSession.asStateFlow()

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, id: String) {
            _activeSession.value = session
            Timber.d("Cast session started: $id")
        }
        override fun onSessionEnded(session: CastSession, error: Int) {
            _activeSession.value = null
        }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _activeSession.value = session
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Timber.w("Cast session start failed: $error")
        }
        override fun onSessionResuming(session: CastSession, id: String) {}
    }

    fun registerSessionListener() {
        runCatching {
            CastContext.getSharedInstance(context)
                .sessionManager
                .addSessionManagerListener(sessionListener, CastSession::class.java)
        }
    }

    fun unregisterSessionListener() {
        runCatching {
            CastContext.getSharedInstance(context)
                .sessionManager
                .removeSessionManagerListener(sessionListener, CastSession::class.java)
        }
    }

    fun load(streamUrl: String, title: String, mimeType: String = "video/mp4") {
        val session = _activeSession.value ?: run {
            Timber.w("No active Cast session")
            return
        }
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, title)
        }
        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(metadata)
            .build()
        session.remoteMediaClient?.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()
        )
        Timber.d("Cast load sent: $streamUrl")
    }

    fun pause() { _activeSession.value?.remoteMediaClient?.pause() }
    fun resume() { _activeSession.value?.remoteMediaClient?.play() }
    fun stop() {
        _activeSession.value?.remoteMediaClient?.stop()
        runCatching {
            CastContext.getSharedInstance(context).sessionManager.endCurrentSession(true)
        }
        _activeSession.value = null
    }

    val isConnected: Boolean get() = _activeSession.value != null
}
