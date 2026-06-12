package com.popcorntime.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.popcorntime.android.domain.model.TorrentSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SourceSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var jackettUrlInput by remember(state.jackettUrl) { mutableStateOf(state.jackettUrl) }
    var jackettApiKeyInput by remember(state.jackettApiKey) { mutableStateOf(state.jackettApiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Torrent Sources") },
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
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Movie Source ──────────────────────────────────────────────────
            Text("Movie source", style = MaterialTheme.typography.titleMedium)
            Column {
                listOf(TorrentSource.YTS, TorrentSource.JACKETT).forEach { source ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(
                            selected = state.movieSource == source,
                            onClick = { viewModel.setMovieSource(source) },
                        )
                        Text(
                            // YTS is the legacy enum name (persisted in prefs); the default
                            // source now talks to the Popcorn Time (Butter) mirror servers.
                            text = if (source == TorrentSource.YTS) "Popcorn Time (default)" else source.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Show Source ───────────────────────────────────────────────────
            Text("Show source", style = MaterialTheme.typography.titleMedium)
            Column {
                listOf(TorrentSource.EZTV, TorrentSource.JACKETT).forEach { source ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(
                            selected = state.showSource == source,
                            onClick = { viewModel.setShowSource(source) },
                        )
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            // ── Jackett/Prowlarr Config ───────────────────────────────────────
            if (state.movieSource == TorrentSource.JACKETT || state.showSource == TorrentSource.JACKETT) {
                HorizontalDivider()
                Text("Jackett / Prowlarr", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Jackett/Prowlarr provides access to many more torrent indexers. Enter your server URL and API key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = jackettUrlInput,
                    onValueChange = { jackettUrlInput = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://localhost:9117") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = jackettApiKeyInput,
                    onValueChange = { jackettApiKeyInput = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = { viewModel.saveJackettConfig(jackettUrlInput, jackettApiKeyInput) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save")
                }
                if (state.isSaved) {
                    Text(
                        "Saved!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
