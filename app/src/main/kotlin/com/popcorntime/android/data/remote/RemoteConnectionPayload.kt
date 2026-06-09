package com.popcorntime.android.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RemoteConnectionPayload(
    val ip: String,
    val port: Int,
    val token: String,
)
