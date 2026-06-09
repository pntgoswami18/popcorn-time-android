package com.popcorntime.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppearanceSettings: () -> Unit,
    onTorrentSettings: () -> Unit,
    onSourceSettings: () -> Unit,
    onSubtitleSettings: () -> Unit,
    onRemoteSettings: () -> Unit,
    onTraktSettings: () -> Unit,
    onDownloadsSettings: () -> Unit,
) {
    val items = listOf(
        SettingsItem("Appearance", Icons.Default.Palette, onAppearanceSettings),
        SettingsItem("Torrent Settings", Icons.Default.Tune, onTorrentSettings),
        SettingsItem("Torrent Sources", Icons.Default.Storage, onSourceSettings),
        SettingsItem("OpenSubtitles", Icons.Default.ClosedCaption, onSubtitleSettings),
        SettingsItem("Remote Control", Icons.Default.SettingsRemote, onRemoteSettings),
        SettingsItem("Trakt.tv", Icons.Default.SyncAlt, onTraktSettings),
        SettingsItem("Downloads", Icons.Default.Download, onDownloadsSettings),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(items, key = { it.title }) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    leadingContent = {
                        Icon(item.icon, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = item.onClick),
                )
                HorizontalDivider()
            }
        }
    }
}
