package com.popcorntime.android.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.network.LanIpResolver
import com.popcorntime.android.data.remote.PairingManager
import com.popcorntime.android.data.remote.RemoteControlServer
import com.popcorntime.android.data.remote.RemoteControlTokenStore
import com.popcorntime.android.data.remote.SessionTokenInfo
import com.popcorntime.android.data.remote.RemoteTlsCertificateManager
import com.popcorntime.android.data.remote.buildPairingUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class RemoteSettingsUiState(
    val isEnabled: Boolean = false,
    val token: String = "",
    val port: Int = 8889,
    val ipAddress: String? = null,
    val isTokenCopied: Boolean = false,
    // SHA-256 fingerprint ("sha256:<hex>") of the server's self-signed TLS
    // certificate. Carried in the QR payload and shown for manual
    // verification. Resolved once off the main thread and cached here.
    val certFingerprint: String = "",
    // Pairing. The QR payload only ever carries a short-lived pairing code —
    // never the persistent bearer token.
    val pairingActive: Boolean = false,
    val pairingCode: String? = null,
    val pairingSecondsLeft: Int = 0,
    val qrPayload: String = "",
    val confirmationRequest: PairingManager.ConfirmationRequest? = null,
    val pairingResult: PairingManager.PairingResult? = null,
)

@HiltViewModel
class RemoteSettingsViewModel @Inject constructor(
    private val server: RemoteControlServer,
    private val tokenStore: RemoteControlTokenStore,
    private val lanIpResolver: LanIpResolver,
    private val pairingManager: PairingManager,
    private val tlsCertificateManager: RemoteTlsCertificateManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteSettingsUiState())
    val uiState: StateFlow<RemoteSettingsUiState> = _uiState.asStateFlow()

    private var copyResetJob: Job? = null
    private var ipRefreshJob: Job? = null
    private var pairingTickJob: Job? = null
    private var resultClearJob: Job? = null

    init {
        viewModelScope.launch {
            // Keystore access is blocking I/O; the QR payload waits on this
            // (recomputeQrPayload requires a non-blank fingerprint).
            val fingerprint = withContext(Dispatchers.IO) {
                runCatching { tlsCertificateManager.fingerprintSha256() }
                    .onFailure { Timber.e(it, "Failed to resolve TLS certificate fingerprint") }
                    .getOrDefault("")
            }
            updateState { it.copy(certFingerprint = fingerprint) }
        }
        viewModelScope.launch {
            tokenStore.observeToken().collect { token ->
                val t = if (token.isBlank()) tokenStore.getOrCreateToken() else token
                updateState { it.copy(token = t) }
            }
        }
        viewModelScope.launch {
            server.isAliveFlow.collect { alive ->
                updateState { it.copy(isEnabled = alive) }
                if (alive) {
                    startIpRefresh()
                } else {
                    stopIpRefresh()
                    pairingManager.cancelPairing()
                }
            }
        }
        viewModelScope.launch {
            pairingManager.uiState.collect { pairing ->
                updateState {
                    it.copy(
                        pairingActive = pairing.code != null,
                        pairingCode = pairing.code,
                        pairingSecondsLeft = pairing.secondsLeft,
                        confirmationRequest = pairing.confirmationRequest,
                        pairingResult = pairing.lastResult,
                    )
                }
                if (pairing.code != null) startPairingTicker() else stopPairingTicker()
                if (pairing.lastResult != null) scheduleResultClear()
            }
        }
    }

    // Only writes the persisted preference. RemoteControlServerController
    // (process-scoped, started in PopcornApp) observes it and starts/stops the
    // server; the toggle UI tracks the actual server state via isAliveFlow.
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { tokenStore.setEnabled(enabled) }
    }

    // --- Pairing -------------------------------------------------------------

    fun startPairing() {
        refreshIp()
        pairingManager.startPairing()
    }

    fun cancelPairing() {
        pairingManager.cancelPairing()
    }

    fun confirmPairing() {
        viewModelScope.launch { pairingManager.confirm() }
    }

    fun denyPairing() {
        pairingManager.deny()
    }

    /** Paired devices (one session token each), for the Advanced section list. */
    val pairedDevices: StateFlow<List<SessionTokenInfo>> = tokenStore.observeSessionTokenInfos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun revokePairedDevice(token: String) {
        viewModelScope.launch { tokenStore.revokeSessionToken(token) }
    }

    fun revokeAllPairedDevices() {
        viewModelScope.launch { tokenStore.revokeAllSessionTokens() }
    }

    private fun startPairingTicker() {
        if (pairingTickJob?.isActive == true) return
        pairingTickJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                pairingManager.tick()
            }
        }
    }

    private fun stopPairingTicker() {
        pairingTickJob?.cancel()
        pairingTickJob = null
    }

    private fun scheduleResultClear() {
        if (resultClearJob?.isActive == true) return
        resultClearJob = viewModelScope.launch {
            delay(5_000)
            pairingManager.clearResult()
        }
    }

    // --- Persistent token (Advanced) ------------------------------------------

    fun regenerateToken() {
        viewModelScope.launch {
            val newToken = tokenStore.regenerateToken()
            server.updateToken(newToken)
            updateState { it.copy(token = newToken, isTokenCopied = false) }
        }
    }

    fun copyToken(clipboardManager: ClipboardManager) {
        val token = _uiState.value.token
        val clip = ClipData.newPlainText("Remote Control Token", token)
        clipboardManager.setPrimaryClip(clip)
        updateState { it.copy(isTokenCopied = true) }
        copyResetJob?.cancel()
        copyResetJob = viewModelScope.launch {
            delay(2_000)
            updateState { it.copy(isTokenCopied = false) }
        }
    }

    fun refreshIp() {
        updateState { it.copy(ipAddress = lanIpResolver.getLanIp()) }
    }

    private fun startIpRefresh() {
        ipRefreshJob?.cancel()
        ipRefreshJob = viewModelScope.launch {
            while (true) {
                refreshIp()
                delay(5_000)
            }
        }
    }

    private fun stopIpRefresh() {
        ipRefreshJob?.cancel()
        ipRefreshJob = null
        updateState { it.copy(ipAddress = null) }
    }

    private fun updateState(transform: (RemoteSettingsUiState) -> RemoteSettingsUiState) {
        _uiState.update { recomputeQrPayload(transform(it)) }
    }

    // The QR is only shown while a pairing session is active, and carries the
    // short-lived code plus the TLS certificate fingerprint. If the IP changes
    // mid-session the URL is regenerated with the same code and TTL.
    private fun recomputeQrPayload(state: RemoteSettingsUiState): RemoteSettingsUiState {
        val qrPayload = state.ipAddress
            ?.takeIf { state.pairingCode != null && state.certFingerprint.isNotBlank() }
            ?.let { ip ->
                buildPairingUrl(
                    ip = ip,
                    port = state.port,
                    code = state.pairingCode!!,
                    certFingerprint = state.certFingerprint,
                )
            } ?: ""
        return state.copy(qrPayload = qrPayload)
    }

    override fun onCleared() {
        pairingManager.cancelPairing()
        super.onCleared()
    }
}
