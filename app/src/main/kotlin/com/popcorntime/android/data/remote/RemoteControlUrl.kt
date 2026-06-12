package com.popcorntime.android.data.remote

import java.net.URLEncoder

/**
 * Builds a remote-control URL with the bearer token in the fragment
 * (`#token=...&fp=...`), which browsers never send to the server; the page
 * reads it on load and strips it from the address bar.
 *
 * Not used for the QR code anymore — the QR carries only a short-lived pairing
 * code (see [buildPairingUrl]). This form remains supported by the web page
 * for the manual/advanced flow where the user pastes the persistent token.
 *
 * [certFingerprint] is the server's self-signed TLS certificate fingerprint
 * (`sha256:<hex>`, see [sha256Fingerprint]); clients use it to verify the
 * certificate trust-on-first-use, since the chain is not CA-signed.
 *
 * Tokens are UUIDs (already URL-safe), but every fragment value is
 * percent-encoded defensively in case the formats ever change.
 */
fun buildRemoteControlUrl(ip: String, port: Int, token: String, certFingerprint: String): String {
    return "https://$ip:$port/#token=${fragmentEncode(token)}&fp=${fragmentEncode(certFingerprint)}"
}

/**
 * Builds the URL encoded into the pairing QR code. It carries only the
 * short-lived 6-digit pairing code and the TLS certificate fingerprint —
 * never a token. The remote page exchanges the code for a session token via
 * POST /pair after the user confirms on the phone. Like the token above, the
 * code travels in the URL fragment, which browsers never send to the server.
 */
fun buildPairingUrl(ip: String, port: Int, code: String, certFingerprint: String): String {
    return "https://$ip:$port/#code=${fragmentEncode(code)}&fp=${fragmentEncode(certFingerprint)}"
}

// URLEncoder is form-encoding: spaces become '+', so map them to %20.
private fun fragmentEncode(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
