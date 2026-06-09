package com.popcorntime.android.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.popcorntime.android.data.preferences.TorrentPrefsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentSettingsViewModel @Inject constructor(
    private val torrentPrefsStore: TorrentPrefsStore,
) : ViewModel() {
    val maxDownloadKbps = torrentPrefsStore.maxDownloadKbps
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val maxUploadKbps = torrentPrefsStore.maxUploadKbps
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val seedingRatioLimit = torrentPrefsStore.seedingRatioLimit
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    fun setMaxDownload(v: Int) { viewModelScope.launch { torrentPrefsStore.setMaxDownloadKbps(v) } }
    fun setMaxUpload(v: Int) { viewModelScope.launch { torrentPrefsStore.setMaxUploadKbps(v) } }
    fun setSeedingRatio(v: Float) { viewModelScope.launch { torrentPrefsStore.setSeedingRatioLimit(v) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentSettingsScreen(
    onBack: () -> Unit,
    viewModel: TorrentSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val maxDownload by viewModel.maxDownloadKbps.collectAsStateWithLifecycle()
    val maxUpload by viewModel.maxUploadKbps.collectAsStateWithLifecycle()
    val seedingRatio by viewModel.seedingRatioLimit.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Torrent Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Download speed
            Column {
                Text(
                    "Download Limit: ${if (maxDownload == 0) "Unlimited" else "$maxDownload Kbps"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = maxDownload.toFloat(),
                    onValueChange = { viewModel.setMaxDownload(it.toInt()) },
                    valueRange = 0f..10000f,
                    steps = 99,
                )
            }

            // Upload speed
            Column {
                Text(
                    "Upload Limit: ${if (maxUpload == 0) "Unlimited" else "$maxUpload Kbps"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = maxUpload.toFloat(),
                    onValueChange = { viewModel.setMaxUpload(it.toInt()) },
                    valueRange = 0f..5000f,
                    steps = 49,
                )
            }

            // Seeding ratio
            Column {
                Text(
                    "Seeding Ratio: ${if (seedingRatio == 0f) "Unlimited" else "%.1f".format(seedingRatio)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = seedingRatio,
                    onValueChange = { viewModel.setSeedingRatio(it) },
                    valueRange = 0f..5f,
                )
            }

            HorizontalDivider()

            // Clear cache
            OutlinedButton(
                onClick = {
                    clearTorrentCache(context)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Clear Torrent Cache")
            }
        }
    }
}

private fun clearTorrentCache(context: Context) {
    runCatching { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
    runCatching { context.getExternalFilesDir("torrent_temp")?.deleteRecursively() }
}
