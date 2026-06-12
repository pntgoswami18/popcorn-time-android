package com.popcorntime.android.data.remote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.security.auth.x500.X500Principal

/**
 * SHA-256 fingerprint of a DER-encoded certificate, formatted as
 * `sha256:<lowercase hex>`. This is the value carried in the pairing QR code
 * (`fp` fragment parameter) so a remote client can verify, trust-on-first-use
 * style, that it is talking to this device and not a man-in-the-middle.
 *
 * Kept as a pure top-level function so it is unit-testable on the JVM, where
 * AndroidKeyStore is unavailable.
 */
fun sha256Fingerprint(encodedCertificate: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(encodedCertificate)
    return "sha256:" + digest.joinToString(separator = "") { "%02x".format(it) }
}

/**
 * Owns the self-signed TLS certificate used by [RemoteControlServer].
 *
 * The EC key pair lives in AndroidKeyStore under a fixed alias, so the
 * certificate (and therefore its fingerprint) is stable across app restarts
 * and token regenerations — clients that pinned the fingerprint on first use
 * keep working. The private key never leaves the keystore; TLS handshake
 * signatures are performed inside it.
 *
 * Certificate creation is deliberately isolated behind this class so an
 * alternative provider (e.g. a BouncyCastle-generated PKCS#12 chain) could be
 * swapped in later without touching the server.
 *
 * All methods perform keystore I/O and must be called off the main thread.
 */
@Singleton
class RemoteTlsCertificateManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "remote_control_tls"
        private const val VALIDITY_YEARS = 10

        /** Preference order; filtered against runtime support (TLSv1.3 needs API 29+). */
        private val DESIRED_PROTOCOLS = arrayOf("TLSv1.3", "TLSv1.2")
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    /**
     * Returns the device's remote-control TLS certificate, generating the key
     * pair and self-signed certificate on first call.
     */
    @Synchronized
    fun getOrCreateCertificate(): X509Certificate {
        val keyStore = loadKeyStore()
        (keyStore.getCertificate(KEY_ALIAS) as? X509Certificate)?.let { return it }

        val notBefore = Calendar.getInstance()
        val notAfter = (notBefore.clone() as Calendar).apply { add(Calendar.YEAR, VALIDITY_YEARS) }
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            // DIGEST_NONE alongside SHA-2 digests: TLS stacks may hash the
            // handshake transcript themselves and ask the keystore to sign a
            // precomputed digest.
            .setDigests(
                KeyProperties.DIGEST_NONE,
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512,
            )
            .setCertificateSubject(X500Principal("CN=PopcornTime Remote"))
            .setCertificateSerialNumber(BigInteger(64, SecureRandom()))
            .setCertificateNotBefore(notBefore.time)
            .setCertificateNotAfter(notAfter.time)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE)
            .apply { initialize(spec) }
            .generateKeyPair()

        return loadKeyStore().getCertificate(KEY_ALIAS) as X509Certificate
    }

    /** `sha256:<hex>` fingerprint of [getOrCreateCertificate]. */
    fun fingerprintSha256(): String = sha256Fingerprint(getOrCreateCertificate().encoded)

    /**
     * Server socket factory backed by the AndroidKeyStore key. The default
     * key manager is wrapped so the server always presents the
     * remote-control certificate regardless of what else is in the keystore.
     */
    fun createServerSocketFactory(): SSLServerSocketFactory {
        getOrCreateCertificate()
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(loadKeyStore(), null)
        val delegate = keyManagerFactory.keyManagers
            .filterIsInstance<X509ExtendedKeyManager>()
            .firstOrNull()
            ?: error("No X509ExtendedKeyManager available for AndroidKeyStore")
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(FixedAliasKeyManager(delegate, KEY_ALIAS)), null, null)
        return sslContext.serverSocketFactory
    }

    /**
     * TLSv1.2/1.3 only, intersected with what the platform supports.
     * Empty result means "leave the platform default enabled set alone".
     */
    fun enabledTlsProtocols(): Array<String> {
        val supported = runCatching {
            SSLContext.getDefault().supportedSSLParameters.protocols.toSet()
        }.getOrDefault(emptySet())
        return DESIRED_PROTOCOLS.filter { it in supported }.toTypedArray()
    }

    /** Key manager that always serves [alias], delegating everything else. */
    private class FixedAliasKeyManager(
        private val delegate: X509ExtendedKeyManager,
        private val alias: String,
    ) : X509ExtendedKeyManager() {
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String = alias
        override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String = alias
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> = arrayOf(alias)
        override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String? =
            delegate.chooseClientAlias(keyType, issuers, socket)
        override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?): String? =
            delegate.chooseEngineClientAlias(keyType, issuers, engine)
        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? =
            delegate.getClientAliases(keyType, issuers)
        override fun getCertificateChain(alias: String?): Array<X509Certificate>? = delegate.getCertificateChain(alias)
        override fun getPrivateKey(alias: String?): PrivateKey? = delegate.getPrivateKey(alias)
    }
}
