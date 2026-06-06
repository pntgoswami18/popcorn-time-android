package com.popcorntime.android.data.cast

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.widget.Toast
import com.popcorntime.android.domain.model.CastState
import com.popcorntime.android.domain.model.CastTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastManager @Inject constructor(
    private val context: Context,
    private val kodiCaster: KodiCaster,
    private val dlnaCaster: DlnaCaster,
    val dlnaDiscovery: DlnaDiscovery,
    val chromeCaster: ChromecastCaster,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _castState = MutableStateFlow<CastState>(CastState.Idle)
    val castState: StateFlow<CastState> = _castState.asStateFlow()

    /** Replaces 127.0.0.1 with the device's real LAN IP so external receivers can reach NanoHTTPD. */
    fun toLanUrl(streamUrl: String): String {
        val wifiManager = context.getSystemService(WifiManager::class.java) ?: return streamUrl
        val ip = wifiManager.connectionInfo?.ipAddress ?: return streamUrl
        if (ip == 0) return streamUrl
        val lanIp = "${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"
        return streamUrl.replace("127.0.0.1", lanIp)
    }

    fun castToChromecast(streamUrl: String, title: String) {
        val url = toLanUrl(streamUrl)
        chromeCaster.load(url, title)
        _castState.value = CastState.Connected(CastTarget.Chromecast(deviceName = title))
    }

    fun castToExternalPlayer(streamUrl: String) {
        // 127.0.0.1 is fine for external players on the same device
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse(streamUrl), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            _castState.value = CastState.Connected(CastTarget.ExternalPlayer)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "No video player app found", Toast.LENGTH_SHORT).show()
            _castState.value = CastState.Error("No external player installed")
        }
    }

    fun castToKodi(streamUrl: String, host: String, port: Int) {
        val url = toLanUrl(streamUrl)
        scope.launch {
            val result = kodiCaster.openUrl(host, port, url)
            _castState.value = if (result.isSuccess) {
                CastState.Connected(CastTarget.Kodi(host, port))
            } else {
                CastState.Error(result.exceptionOrNull()?.message ?: "Kodi error")
            }
        }
    }

    fun castToDlna(streamUrl: String, renderer: DlnaRenderer) {
        val url = toLanUrl(streamUrl)
        scope.launch {
            val result = dlnaCaster.playUrl(renderer, url)
            _castState.value = if (result.isSuccess) {
                CastState.Connected(CastTarget.Dlna(renderer.name))
            } else {
                CastState.Error(result.exceptionOrNull()?.message ?: "DLNA error")
            }
        }
    }

    fun disconnect() {
        chromeCaster.stop()
        _castState.value = CastState.Idle
    }
}
