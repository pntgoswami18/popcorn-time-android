package com.popcorntime.android.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanIpResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Suppress("DEPRECATION")
    fun getLanIp(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
            val network = cm.activeNetwork ?: return null
            val linkProps = cm.getLinkProperties(network) ?: return null
            linkProps.linkAddresses
                .map { it.address }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        } else {
            val wifiManager = context.getSystemService(android.net.wifi.WifiManager::class.java) ?: return null
            val ip = wifiManager.connectionInfo?.ipAddress ?: return null
            if (ip == 0) return null
            "${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"
        }
    }
}
