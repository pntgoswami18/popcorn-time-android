package com.popcorntime.android.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.network.LanIpResolver
import com.popcorntime.android.data.remote.RemoteConnectionPayload
import com.popcorntime.android.data.remote.RemoteControlServer
import com.popcorntime.android.data.remote.RemoteControlTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class RemoteSettingsUiState(
    val isEnabled: Boolean = false,
    val token: String = "",
    val port: Int = 8889,
    val ipAddress: String? = null,
    val qrPayload: String = "",
    val isTokenCopied: Boolean = false,
)

@HiltViewModel
class RemoteSettingsViewModel @Inject constructor(
    private val server: RemoteControlServer,
    private val tokenStore: RemoteControlTokenStore,
    private val lanIpResolver: LanIpResolver,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteSettingsUiState())
    val uiState: StateFlow<RemoteSettingsUiState> = _uiState.asStateFlow()

    private var copyResetJob: Job? = null
    private var ipRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStore.observeToken().collect { token ->
                val t = if (token.isBlank()) tokenStore.getOrCreateToken() else token
                updateState { it.copy(token = t) }
            }
        }
        viewModelScope.launch {
            server.isAliveFlow.collect { alive ->
                updateState { it.copy(isEnabled = alive) }
                if (alive) startIpRefresh() else stopIpRefresh()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            viewModelScope.launch {
                val token = tokenStore.getOrCreateToken()
                server.startIfNotRunning(token)
            }
        } else {
            server.stopIfRunning()
        }
    }

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

    private fun recomputeQrPayload(state: RemoteSettingsUiState): RemoteSettingsUiState {
        val qrPayload = state.ipAddress?.takeIf { state.token.isNotBlank() }?.let { ip ->
            json.encodeToString(RemoteConnectionPayload(ip = ip, port = state.port, token = state.token))
        } ?: ""
        return state.copy(qrPayload = qrPayload)
    }
}
