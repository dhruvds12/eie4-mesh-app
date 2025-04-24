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
    // 1) Collect the connection state (nullable until it emits)
    val state by viewModel.connectionState
        .collectAsState(initial = null)

    // 2) Build up a local list of messages
    val messages = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) {
        viewModel.incomingText.collect { msg ->
            messages += msg
        }
    }

    var draft by remember { mutableStateOf("") }

    // Kick off the GATT connect when we land here
    LaunchedEffect(address) {
        viewModel.connect(address)
    }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        // Show status / chat area
        when (val e = state) {
            GattConnectionEvent.Connecting     -> Text("🔄 Connecting…")
            GattConnectionEvent.Connected      -> Text("✅ Connected! waiting for services…")
            GattConnectionEvent.ServicesDiscovered -> {
                Text("✅ Services discovered!")
                Spacer(Modifier.height(8.dp))

                // 3) Display the chat history
                LazyColumn(Modifier.weight(1f)) {
                    items(messages) { msg ->
                        Text(msg, modifier = Modifier.padding(4.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 4) Input + send
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        viewModel.send(draft)
                        draft = ""
                    }) {
                        Text("Send")
                    }
                }
            }
            GattConnectionEvent.Disconnected -> Text("⚠️ Disconnected")
            is GattConnectionEvent.CharacteristicRead -> {
                // ignore
            }
            is GattConnectionEvent.WriteCompleted -> {
                // ignore
            }
            is GattConnectionEvent.Error -> Text("❌ Error: ${e.reason}")
            null -> Text("Waiting to connect…")
        }

        Spacer(Modifier.height(16.dp))

        // Always allow disconnect
        Button(onClick = { viewModel.disconnect() }, modifier = Modifier.fillMaxWidth()) {
            Text("Disconnect")
        }
    }
}
