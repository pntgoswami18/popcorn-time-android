package com.popcorntime.android.data.cast

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
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

class CastManager constructor(
    private val context: Context,
    private val kodiCaster: KodiCaster,
    private val dlnaCaster: DlnaCaster,
    val dlnaDiscovery: DlnaDiscovery,
    val chromeCaster: ChromecastCaster,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _castState = MutableStateFlow<CastState>(CastState.Idle)
    val castState: StateFlow<CastState> = _castState.asStateFlow()

    /** Tracks the last active DLNA renderer for cleanup in disconnect(). */
    private var activeDlnaRenderer: DlnaRenderer? = null

    /** Tracks the last active Kodi host/port for cleanup in disconnect(). */
    private var activeKodiHost: String? = null
    private var activeKodiPort: Int = 8080

    /** Replaces 127.0.0.1 with the device's real LAN IP so external receivers can reach NanoHTTPD. */
    @Suppress("DEPRECATION")
    fun toLanUrl(streamUrl: String): String {
        val lanIp = getLanIp() ?: return streamUrl
        return streamUrl.replace("127.0.0.1", lanIp)
    }

    private fun getLanIp(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: use ConnectivityManager
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
            val network = cm.activeNetwork ?: return null
            val linkProps = cm.getLinkProperties(network) ?: return null
            linkProps.linkAddresses
                .map { it.address }
                .filterIsInstance<java.net.Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        } else {
            // API 26–30: WifiManager still works
            @Suppress("DEPRECATION")
            val wifiManager = context.getSystemService(android.net.wifi.WifiManager::class.java) ?: return null
            @Suppress("DEPRECATION")
            val ip = wifiManager.connectionInfo?.ipAddress ?: return null
            if (ip == 0) return null
            "${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"
        }
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
        activeKodiHost = host
        activeKodiPort = port
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
        activeDlnaRenderer = renderer
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
        val kodiHost = activeKodiHost
        if (kodiHost != null) {
            scope.launch { kodiCaster.stop(kodiHost, activeKodiPort) }
            activeKodiHost = null
        }
        val dlnaRenderer = activeDlnaRenderer
        if (dlnaRenderer != null) {
            scope.launch { dlnaCaster.stop(dlnaRenderer) }
            activeDlnaRenderer = null
        }
        _castState.value = CastState.Idle
    }
}
