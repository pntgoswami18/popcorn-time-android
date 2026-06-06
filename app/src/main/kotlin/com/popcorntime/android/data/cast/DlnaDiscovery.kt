package com.popcorntime.android.data.cast

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

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
        val found = mutableListOf<DlnaRenderer>()

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, code: Int) {
                Timber.w("DLNA resolve failed: $code")
            }
            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = info.host?.hostAddress ?: return
                val renderer = DlnaRenderer(
                    name = info.serviceName,
                    host = host,
                    port = info.port,
                )
                found.removeAll { it.name == renderer.name }
                found.add(renderer)
                _renderers.value = found.toList()
                Timber.d("DLNA renderer found: ${renderer.name} @ $host:${info.port}")
            }
        }

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
                try {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(info, resolveListener)
                } catch (e: Exception) {
                    Timber.w(e, "DLNA resolve enqueue failed")
                }
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                found.removeAll { it.name == info.serviceName }
                _renderers.value = found.toList()
            }
        }

        try {
            nsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
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
