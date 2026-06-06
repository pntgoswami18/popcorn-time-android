package com.popcorntime.android.ui.cast

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun KodiDialog(
    initialHost: String = "",
    initialPort: Int = 8080,
    onConfirm: (host: String, port: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var host by remember { mutableStateOf(initialHost) }
    var portText by remember { mutableStateOf(if (initialPort > 0) initialPort.toString() else "8080") }
    val portValid = portText.toIntOrNull()?.let { it in 1..65535 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cast to Kodi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text("Kodi IP address") },
                    placeholder = { Text("192.168.1.x") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.trim() },
                    label = { Text("Port") },
                    placeholder = { Text("8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = portText.isNotEmpty() && !portValid,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(host, portText.toIntOrNull() ?: 8080) },
                enabled = host.isNotBlank() && portValid,
            ) { Text("Connect") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
