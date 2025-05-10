// feature‑ble/ui/NewUserChatDialog.kt
package com.example.disastermesh.feature.ble.ui.model

import androidx.compose.material3.*
import androidx.compose.runtime.*

/**
 * A one‑shot dialog that lets the user type a phone number and calls [onCreate]
 * with the string once the user presses “Create”.
 *
 * Caller controls its visibility by toggling [show] and should reset [show]
 * to false in either callback.
 */
@Composable
fun NewUserChatDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    if (!show) return

    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onCreate(phone) },
                enabled = phone.startsWith("+") && phone.length >= 4
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("New user chat") },
        text = {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (+CC)") }
            )
        }
    )
}
