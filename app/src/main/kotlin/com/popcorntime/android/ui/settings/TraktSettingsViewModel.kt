package com.popcorntime.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.api.dto.TraktAuthState
import com.popcorntime.android.data.trakt.TraktAuthService
import com.popcorntime.android.data.trakt.TraktTokenStore
import com.popcorntime.android.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TraktSettingsUiState(
    val isLoggedIn: Boolean = false,
    val userCode: String? = null,
    val verificationUrl: String? = null,
    val isPolling: Boolean = false,
    val loginError: String? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
)

@HiltViewModel
class TraktSettingsViewModel @Inject constructor(
    private val traktAuthService: TraktAuthService,
    private val traktTokenStore: TraktTokenStore,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TraktSettingsUiState())
    val uiState: StateFlow<TraktSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            traktTokenStore.isTraktConnected().collect { connected ->
                _uiState.update { it.copy(isLoggedIn = connected) }
            }
        }
    }

    fun startLogin() {
        viewModelScope.launch {
            _uiState.update { it.copy(loginError = null, userCode = null, verificationUrl = null) }
            try {
                val codeResponse = traktAuthService.requestDeviceCode()
                _uiState.update {
                    it.copy(
                        userCode = codeResponse.userCode,
                        verificationUrl = codeResponse.verificationUrl,
                        isPolling = true,
                    )
                }
                traktAuthService.pollForToken(
                    deviceCode = codeResponse.deviceCode,
                    interval = codeResponse.interval,
                    expiresIn = codeResponse.expiresIn,
                ).collect { authState ->
                    when (authState) {
                        is TraktAuthState.Authorized -> {
                            traktTokenStore.saveToken(
                                accessToken = authState.token.accessToken,
                                refreshToken = authState.token.refreshToken,
                                expiresIn = authState.token.expiresIn,
                                createdAt = authState.token.createdAt,
                            )
                            _uiState.update { it.copy(isPolling = false, userCode = null, verificationUrl = null) }
                        }
                        is TraktAuthState.Expired ->
                            _uiState.update { it.copy(isPolling = false, loginError = "Code expired. Please try again.") }
                        is TraktAuthState.Error ->
                            _uiState.update { it.copy(isPolling = false, loginError = authState.message) }
                        TraktAuthState.Pending -> { /* keep polling state */ }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPolling = false, loginError = e.message ?: "Unknown error") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { traktTokenStore.clearToken() }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            try {
                libraryRepository.syncFromTrakt()
                _uiState.update { it.copy(isSyncing = false, syncMessage = "Sync complete!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, syncMessage = "Sync failed: ${e.message}") }
            }
        }
    }
}
