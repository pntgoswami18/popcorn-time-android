package com.popcorntime.android.ui.settings

import android.app.Activity
import android.content.ClipboardManager
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorntime.android.data.remote.PairingManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.popcorntime.android.ui.components.QrCodeImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteSettingsScreen(
    onBack: () -> Unit,
    viewModel: RemoteSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(state.isEnabled) {
        val window = (context as? Activity)?.window
        if (state.isEnabled) {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = null)
                        Text("Remote Control")
                    }
                },
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "Enable remote control (HTTPS)",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "Allow external apps to control playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = viewModel::setEnabled,
                    )
                }
            }

            if (state.isEnabled) {
                Text(
                    "Anyone on your network who can see this screen can control playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("IP Address", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            state.ipAddress ?: "Unavailable",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = if (state.ipAddress != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Port", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${state.port}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // --- Pairing card -------------------------------------------
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.pairingActive) {
                            Text("Scan to pair", style = MaterialTheme.typography.titleSmall)
                            if (state.qrPayload.isNotBlank()) {
                                QrCodeImage(
                                    content = state.qrPayload,
                                    modifier = Modifier.size(200.dp),
                                )
                            }
                            Text(
                                state.pairingCode ?: "",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 6.sp,
                                ),
                            )
                            Text(
                                "Scan with your phone camera, or open the remote page and enter this code manually. " +
                                    "Expires in ${state.pairingSecondsLeft / 60}:${"%02d".format(state.pairingSecondsLeft % 60)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick = viewModel::cancelPairing,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Cancel pairing")
                            }
                        } else {
                            Text("Pair a new device", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Generates a short-lived QR code. The device only gets access after you approve it on this screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = viewModel::startPairing,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Pair a new device")
                            }
                            when (state.pairingResult) {
                                PairingManager.PairingResult.PAIRED -> Text(
                                    "Device paired",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                PairingManager.PairingResult.DENIED -> Text(
                                    "Pairing request denied",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                PairingManager.PairingResult.EXPIRED -> Text(
                                    "Pairing code expired",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                null -> {}
                            }
                        }
                    }
                }

                // --- Confirmation dialog -------------------------------------
                state.confirmationRequest?.let { request ->
                    AlertDialog(
                        onDismissRequest = viewModel::denyPairing,
                        title = { Text("Allow remote device?") },
                        text = {
                            Text("${request.clientName} at ${request.clientIp} wants to control playback on this device.")
                        },
                        confirmButton = {
                            TextButton(onClick = viewModel::confirmPairing) { Text("Allow") }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::denyPairing) { Text("Deny") }
                        },
                    )
                }

                // --- Advanced: persistent token + revocation ------------------
                var advancedExpanded by rememberSaveable { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Advanced", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { advancedExpanded = !advancedExpanded }) {
                                Icon(
                                    if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (advancedExpanded) "Collapse" else "Expand",
                                )
                            }
                        }
                        if (advancedExpanded) {
                            Text(
                                "API Bearer Token",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                "For scripts and API clients only. Never shared via QR. Keep it secret.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (state.token.isNotBlank()) {
                                Text(
                                    text = state.token,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val clipboardManager = context.getSystemService(ClipboardManager::class.java)
                                OutlinedButton(
                                    onClick = { viewModel.copyToken(clipboardManager) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(if (state.isTokenCopied) "Copied!" else "Copy")
                                }
                                OutlinedButton(
                                    onClick = viewModel::regenerateToken,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Regenerate")
                                }
                            }
                            OutlinedButton(
                                onClick = viewModel::revokeAllPairedDevices,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Revoke all paired devices")
                            }
                            Text(
                                "Certificate fingerprint",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                "Connections use a self-signed TLS certificate. Compare this SHA-256 fingerprint with the one your browser or client reports to verify you are connected to this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.certFingerprint.ifBlank { "Unavailable" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "How to connect",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        val endpoint = state.ipAddress?.let { "https://$it:${state.port}/" } ?: "https://<your-phone-IP>:${state.port}/"
                        Text(
                            "Tap \"Pair a new device\" and scan the QR code with the other device, then approve the request here. " +
                                "The pairing code expires automatically after about 90 seconds. " +
                                "You can also open $endpoint in a browser on this network and enter the code manually. " +
                                "Connections are encrypted with this device's self-signed certificate, so your browser shows a security warning on first connect — accept it once, and use the certificate fingerprint in the Advanced section to verify it. " +
                                "The REST API stays available at the same address using the bearer token from the Advanced section as the Authorization header.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
