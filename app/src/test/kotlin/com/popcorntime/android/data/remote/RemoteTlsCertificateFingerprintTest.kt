package com.popcorntime.android.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * JVM tests for the fingerprint helper used in the pairing QR payload.
 * AndroidKeyStore is not available on the JVM, so the hash/formatting logic
 * is exercised against a fixed PEM certificate fixture whose fingerprint was
 * computed independently with `openssl x509 -fingerprint -sha256`.
 */
class RemoteTlsCertificateFingerprintTest {

    // Self-signed EC P-256 (named curve prime256v1) certificate,
    // CN=PopcornTime Remote Test. The curve must be a named curve — the JDK
    // X.509 parser rejects certificates with explicit EC domain parameters.
    private val fixturePem = """
        -----BEGIN CERTIFICATE-----
        MIIBNDCB2gIJAJhVSmeEcZM+MAoGCCqGSM49BAMCMCIxIDAeBgNVBAMMF1BvcGNv
        cm5UaW1lIFJlbW90ZSBUZXN0MB4XDTI2MDYxMjA1MzMyNVoXDTM2MDYwOTA1MzMy
        NVowIjEgMB4GA1UEAwwXUG9wY29yblRpbWUgUmVtb3RlIFRlc3QwWTATBgcqhkjO
        PQIBBggqhkjOPQMBBwNCAATQDhT8XxHMA4FDsYwJHJAqDRDKCNqpkKoFCKAE+PNG
        qM1d/5U+W+KcaCIMClaGlMWOSjfqouFmuR6ySbuMzBGpMAoGCCqGSM49BAMCA0kA
        MEYCIQDIgxhPdw+BKANgEHBipoO5qDZD4E8C2q+/rI6C5Grn9QIhALDlYQZVeNtM
        4pe+nMpCR9MYVEaG54abYEdweskwkJV7
        -----END CERTIFICATE-----
    """.trimIndent()

    // openssl x509 -fingerprint -sha256 reports:
    // 21:B8:C7:4D:B9:82:72:F0:B6:C7:F5:0D:7F:B0:71:21:D2:77:F9:62:67:3C:0D:EF:B7:B2:20:3F:2C:42:41:ED
    private val expectedHex =
        "21b8c74db98272f0b6c7f50d7fb07121d277f962673c0defb7b2203f2c4241ed"

    private fun fixtureCertificate(): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(fixturePem.byteInputStream()) as X509Certificate

    @Test
    fun `fingerprint matches openssl for fixture certificate`() {
        val fingerprint = sha256Fingerprint(fixtureCertificate().encoded)
        assertEquals("sha256:$expectedHex", fingerprint)
    }

    @Test
    fun `fingerprint format is sha256 prefix plus 64 lowercase hex chars`() {
        val fingerprint = sha256Fingerprint(fixtureCertificate().encoded)
        assertTrue(fingerprint.matches(Regex("^sha256:[0-9a-f]{64}$")))
    }

    @Test
    fun `fingerprint of empty input is hash of empty byte array`() {
        // SHA-256 of the empty string is a well-known constant.
        assertEquals(
            "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Fingerprint(ByteArray(0)),
        )
    }
}
