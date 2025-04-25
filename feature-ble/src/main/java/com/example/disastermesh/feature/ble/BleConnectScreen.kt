package com.example.disastermesh.feature.ble

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.disastermesh.core.ble.GattConnectionEvent

@Composable
fun BleConnectScreen(
    address: String,
    navController: NavController,
    viewModel: BleConnectViewModel = hiltViewModel()
) {
    /* ------------------------------------------------------------------ */
    /*  state                                                             */
    /* ------------------------------------------------------------------ */

    val state by viewModel.connectionState.collectAsState()
    val chat  by viewModel.chat.collectAsState()

    var draft by remember { mutableStateOf("") }

    /* start connecting once when we enter the screen */
    LaunchedEffect(address) { viewModel.connect(address) }

    /* ------------------------------------------------------------------ */
    /*  UI                                                                 */
    /* ------------------------------------------------------------------ */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        /* top status line */
        StatusLine(state)

        Spacer(Modifier.height(8.dp))

        /* chat window — visible once services have been discovered
           and *stays* visible for every later event such as WriteCompleted */
        val readyForChat = when (state) {
            GattConnectionEvent.ServicesDiscovered     -> true
            is GattConnectionEvent.WriteCompleted      -> true
            is GattConnectionEvent.CharacteristicRead  -> true
            else                                       -> false
        }

        if (readyForChat) {
            LazyColumn(Modifier.weight(1f)) {
                items(chat) { msg ->
                    val prefix = if (msg.fromMe) "Me: " else "Node: "
                    Text(prefix + msg.text, modifier = Modifier.padding(4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") }
                )
                Button(onClick = {
                    viewModel.send(draft)
                    draft = ""
                }) { Text("Send") }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.disconnect() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Disconnect") }
    }
}

@Composable
private fun StatusLine(state: GattConnectionEvent?) = when (state) {
    GattConnectionEvent.Connecting      -> Text("🔄 Connecting…")
    GattConnectionEvent.Connected       -> Text("✅ Connected – discovering services…")
    GattConnectionEvent.ServicesDiscovered -> Text("✅ Ready – chat below")
    GattConnectionEvent.Disconnected    -> Text("⚠️ Disconnected")
    is GattConnectionEvent.Error        -> Text("❌ Error: ${state.reason}")
    else                                -> Text("Waiting…")
}
