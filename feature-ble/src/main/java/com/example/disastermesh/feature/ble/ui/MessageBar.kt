package com.example.disastermesh.feature.ble.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.disastermesh.core.ble.MAX_MSG_CHARS


@Composable
fun MessageBar(
    text        : String,
    onTextChange: (String) -> Unit,
    onSend      : () -> Unit,
    enabled     : Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {      // ❶ surround with Row
        TextField(
            value = text,
            onValueChange = { new ->
                if (new.length <= MAX_MSG_CHARS) onTextChange(new)
            },
            modifier        = Modifier.weight(1f),                  // ❷ RowScope-weight is OK now
            placeholder     = { Text("Type… ($MAX_MSG_CHARS)") },
            enabled         = enabled,
            supportingText  = { Text("${text.length} / $MAX_MSG_CHARS") }
        )

        Button(
            onClick  = onSend,
            enabled  = enabled && text.isNotBlank()
        ) { Text("Send") }
    }
}
