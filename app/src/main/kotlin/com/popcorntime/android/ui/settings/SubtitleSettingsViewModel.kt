package com.popcorntime.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.subtitles.OsAuthService
import com.popcorntime.android.data.subtitles.OsLoginResult
import com.popcorntime.android.data.subtitles.OsTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubtitleSettingsUiState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val allowedDownloads: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val preferredLanguages: List<String> = listOf("en"),
)

@HiltViewModel
class SubtitleSettingsViewModel @Inject constructor(
    private val osAuthService: OsAuthService,
    private val osTokenStore: OsTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubtitleSettingsUiState())
    val uiState: StateFlow<SubtitleSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            osTokenStore.isLoggedIn().collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
            }
        }
        viewModelScope.launch {
            val languages = osTokenStore.getPreferredLanguages()
            val username = osTokenStore.getUsername() ?: ""
            _uiState.update { it.copy(preferredLanguages = languages, username = username) }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = osAuthService.login(username.trim(), password)) {
                is OsLoginResult.Success -> {
                    osTokenStore.saveToken(
                        username = username.trim(),
                        token = result.token,
                        baseUrl = result.baseUrl,
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            username = username.trim(),
                            allowedDownloads = result.allowedDownloads,
                            error = null,
                        )
                    }
                }
                is OsLoginResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.reason) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val token = osTokenStore.getToken()
            if (!token.isNullOrBlank()) {
                osAuthService.logout(token)
            }
            osTokenStore.clearToken()
            _uiState.update { it.copy(isLoading = false, username = "", allowedDownloads = 0) }
        }
    }

    fun saveLanguages(languages: List<String>) {
        viewModelScope.launch {
            osTokenStore.savePreferredLanguages(languages)
            _uiState.update { it.copy(preferredLanguages = languages) }
        }
    }
}
