package com.popcorntime.android.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

data class RemoteSettingsUiState(
    val isEnabled: Boolean = false,
    val token: String = "",
    val port: Int = 8889,
    val isTokenCopied: Boolean = false,
)

@HiltViewModel
class RemoteSettingsViewModel @Inject constructor(
    private val server: RemoteControlServer,
    private val tokenStore: RemoteControlTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteSettingsUiState())
    val uiState: StateFlow<RemoteSettingsUiState> = _uiState.asStateFlow()

    private var copyResetJob: Job? = null

    init {
        // Observe the stored token
        viewModelScope.launch {
            tokenStore.observeToken().collect { token ->
                // If empty, trigger creation
                val t = if (token.isBlank()) tokenStore.getOrCreateToken() else token
                _uiState.update { it.copy(token = t) }
            }
        }
        // Collect live server alive state
        viewModelScope.launch {
            server.isAliveFlow.collect { alive ->
                _uiState.update { it.copy(isEnabled = alive) }
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
            _uiState.update { it.copy(token = newToken, isTokenCopied = false) }
        }
    }

    fun copyToken(clipboardManager: ClipboardManager) {
        val token = _uiState.value.token
        val clip = ClipData.newPlainText("Remote Control Token", token)
        clipboardManager.setPrimaryClip(clip)
        _uiState.update { it.copy(isTokenCopied = true) }
        copyResetJob?.cancel()
        copyResetJob = viewModelScope.launch {
            delay(2_000)
            _uiState.update { it.copy(isTokenCopied = false) }
        }
    }
}
