package com.popcorntime.android.data.cast

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executors

data class DlnaRenderer(
    val name: String,
    val host: String,
    val port: Int,
)

class DlnaDiscovery(private val context: Context) {

    private val _renderers = MutableStateFlow<List<DlnaRenderer>>(emptyList())
    val renderers: StateFlow<List<DlnaRenderer>> = _renderers.asStateFlow()

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(NsdManager::class.java)
    }

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (discoveryListener != null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(type: String, code: Int) {
                Timber.w("DLNA discovery start failed: $code")
            }
            override fun onStopDiscoveryFailed(type: String, code: Int) {
                Timber.w("DLNA discovery stop failed: $code")
            }
            override fun onDiscoveryStarted(type: String) {
                Timber.d("DLNA discovery started")
            }
            override fun onDiscoveryStopped(type: String) {
                Timber.d("DLNA discovery stopped")
                discoveryListener = null
            }
            override fun onServiceFound(info: NsdServiceInfo) {
                val perCallResolver = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(i: NsdServiceInfo, code: Int) {
                        Timber.w("DLNA resolve failed: $code for ${i.serviceName}")
                    }
                    override fun onServiceResolved(i: NsdServiceInfo) {
                        val host = i.host?.hostAddress ?: return
                        val renderer = DlnaRenderer(name = i.serviceName, host = host, port = i.port)
                        val current = _renderers.value.toMutableList()
                        current.removeAll { it.name == renderer.name }
                        current.add(renderer)
                        _renderers.value = current
                        Timber.d("DLNA renderer found: ${renderer.name} @ $host:${i.port}")
                    }
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        nsdManager.resolveService(info, Executors.newSingleThreadExecutor(), perCallResolver)
                    } else {
                        @Suppress("DEPRECATION")
                        nsdManager.resolveService(info, perCallResolver)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "DLNA resolve enqueue failed: ${info.serviceName}")
                }
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                val current = _renderers.value.toMutableList()
                current.removeAll { it.name == info.serviceName }
                _renderers.value = current
            }
        }

        try {
            nsdManager.discoverServices("_upnp._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Timber.w(e, "DLNA discoverServices failed")
            discoveryListener = null
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try { nsdManager.stopServiceDiscovery(it) } catch (e: Exception) { /* ignore */ }
        }
        discoveryListener = null
        _renderers.value = emptyList()
    }
}
