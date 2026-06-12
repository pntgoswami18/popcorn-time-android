package com.popcorntime.android.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteControlUrlTest {

    private val fp = "sha256:" + "ab".repeat(32)

    // ':' percent-encodes to %3A in the fragment.
    private val fpEncoded = "sha256%3A" + "ab".repeat(32)

    @Test
    fun `normal uuid token produces https url with fingerprint`() {
        val url = buildRemoteControlUrl(
            ip = "192.168.1.42",
            port = 8889,
            token = "123e4567-e89b-12d3-a456-426614174000",
            certFingerprint = fp,
        )
        assertEquals(
            "https://192.168.1.42:8889/#token=123e4567-e89b-12d3-a456-426614174000&fp=$fpEncoded",
            url,
        )
    }

    @Test
    fun `token with reserved characters is percent encoded`() {
        val url = buildRemoteControlUrl(ip = "10.0.0.5", port = 8889, token = "a&b=c#d e/f?g", certFingerprint = fp)
        assertEquals("https://10.0.0.5:8889/#token=a%26b%3Dc%23d%20e%2Ff%3Fg&fp=$fpEncoded", url)
    }

    @Test
    fun `token travels in fragment not query`() {
        val url = buildRemoteControlUrl(ip = "192.168.0.10", port = 8889, token = "abc", certFingerprint = fp)
        assertFalse("token must not appear in the query string", url.contains("?token="))
        assertTrue("token must appear in the fragment", url.contains("/#token=abc"))
        // Fragment must come after host:port/ with no path or query in between.
        assertEquals("https://192.168.0.10:8889/", url.substringBefore('#'))
    }

    @Test
    fun `pairing url carries code and fingerprint in fragment`() {
        val url = buildPairingUrl(ip = "192.168.1.42", port = 8889, code = "123456", certFingerprint = fp)
        assertEquals("https://192.168.1.42:8889/#code=123456&fp=$fpEncoded", url)
    }

    @Test
    fun `pairing url never contains a token parameter`() {
        val url = buildPairingUrl(ip = "10.0.0.5", port = 8889, code = "654321", certFingerprint = fp)
        assertFalse("pairing url must not contain a token", url.contains("token"))
        assertEquals("https://10.0.0.5:8889/", url.substringBefore('#'))
    }

    @Test
    fun `urls always use the https scheme`() {
        val pairing = buildPairingUrl(ip = "10.0.0.5", port = 8889, code = "654321", certFingerprint = fp)
        val token = buildRemoteControlUrl(ip = "10.0.0.5", port = 8889, token = "t", certFingerprint = fp)
        assertTrue(pairing.startsWith("https://"))
        assertTrue(token.startsWith("https://"))
        assertFalse(pairing.startsWith("http://"))
        assertFalse(token.startsWith("http://"))
    }

    @Test
    fun `fingerprint decodes back to original value`() {
        val url = buildPairingUrl(ip = "10.0.0.5", port = 8889, code = "654321", certFingerprint = fp)
        val fragment = url.substringAfter('#')
        val fpParam = fragment.split('&').first { it.startsWith("fp=") }.removePrefix("fp=")
        assertEquals(fp, java.net.URLDecoder.decode(fpParam, Charsets.UTF_8.name()))
    }

    @Test
    fun `port is included verbatim`() {
        val url = buildRemoteControlUrl(ip = "192.168.0.10", port = 1234, token = "t", certFingerprint = fp)
        assertTrue(url.startsWith("https://192.168.0.10:1234/#"))
    }
}
