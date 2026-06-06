package com.popcorntime.android.domain.model

sealed class CastTarget {
    data class Chromecast(val deviceName: String) : CastTarget()
    data object ExternalPlayer : CastTarget()
    data class Kodi(val host: String, val port: Int) : CastTarget()
    data class Dlna(val rendererName: String) : CastTarget()
}

sealed class CastState {
    data object Idle : CastState()
    data class Connected(val target: CastTarget) : CastState()
    data class Error(val message: String) : CastState()
}
