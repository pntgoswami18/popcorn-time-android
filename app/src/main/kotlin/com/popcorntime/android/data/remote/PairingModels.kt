package com.popcorntime.android.data.remote

import kotlinx.serialization.Serializable

/** Body of POST /pair. */
@Serializable
data class PairRequest(
    val code: String,
    val clientName: String? = null,
)

/** 202 response of POST /pair. */
@Serializable
data class PairStartResponse(
    val pairingId: String,
    val status: String = "awaiting_confirmation",
)

/** 200 response of GET /pair/status. Token present exactly once, on confirmation. */
@Serializable
data class PairStatusResponse(
    val status: String,
    val token: String? = null,
)

/** Error envelope shared by the pairing endpoints. */
@Serializable
data class PairErrorResponse(val error: String)
