package com.popcorntime.android.ui.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.popcorntime.android.data.cast.DlnaRenderer
import com.popcorntime.android.domain.model.CastState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastBottomSheet(
    castState: CastState,
    kodiAddress: Pair<String, Int>,
    dlnaRenderers: List<DlnaRenderer>,
    onChromecastClick: () -> Unit,
    onExternalPlayerClick: () -> Unit,
    onKodiConnect: (host: String, port: Int) -> Unit,
    onDlnaSelect: (DlnaRenderer) -> Unit,
    onDismiss: () -> Unit,
) {
    var showKodiDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            // Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cast to…", style = MaterialTheme.typography.titleMedium)
                if (castState is CastState.Connected) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "Connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            HorizontalDivider()

            // Chromecast
            CastRow(
                icon = Icons.Default.Cast,
                title = "Chromecast",
                subtitle = "Stream to a nearby Cast device",
                onClick = { onChromecastClick(); onDismiss() },
            )

            // External player
            CastRow(
                icon = Icons.Default.OpenInNew,
                title = "External Player",
                subtitle = "VLC, MX Player, mpv, or any video app",
                onClick = { onExternalPlayerClick(); onDismiss() },
            )

            // Kodi
            val kodiLabel = if (kodiAddress.first.isNotBlank())
                "Kodi @ ${kodiAddress.first}:${kodiAddress.second}"
            else "Set up Kodi"
            CastRow(
                icon = Icons.Default.Tv,
                title = "Kodi / XBMC",
                subtitle = kodiLabel,
                onClick = { showKodiDialog = true },
            )

            // DLNA
            if (dlnaRenderers.isEmpty()) {
                CastRow(
                    icon = Icons.Default.ScreenShare,
                    title = "DLNA",
                    subtitle = "Scanning for renderers…",
                    onClick = {},
                    enabled = false,
                )
            } else {
                dlnaRenderers.forEach { renderer ->
                    CastRow(
                        icon = Icons.Default.ScreenShare,
                        title = renderer.name,
                        subtitle = "DLNA · ${renderer.host}",
                        onClick = { onDlnaSelect(renderer); onDismiss() },
                    )
                }
            }
        }
    }

    if (showKodiDialog) {
        KodiDialog(
            initialHost = kodiAddress.first,
            initialPort = kodiAddress.second,
            onConfirm = { host, port ->
                showKodiDialog = false
                onKodiConnect(host, port)
                onDismiss()
            },
            onDismiss = { showKodiDialog = false },
        )
    }
}

@Composable
private fun CastRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.5f,
                ),
            )
        }
    }
}
