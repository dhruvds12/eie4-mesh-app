package com.example.disastermesh.feature.ble.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.feature.ble.nav.Screen
import kotlinx.coroutines.launch

@Composable
fun LandingScreen(
    nav: NavController,
    vm : LandingViewModel = hiltViewModel()
) {
    val state by vm.connection.collectAsState()

    val connected = when (state) {
        GattConnectionEvent.ServicesDiscovered     -> true
        is GattConnectionEvent.WriteCompleted      -> true
        is GattConnectionEvent.CharacteristicRead  -> true
        else                                       -> false
    }

    val inFlight  = state == GattConnectionEvent.Connecting ||
            state == GattConnectionEvent.Connected

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* status text */
        Text(
            when (state) {
                null                             -> "Not connected"
                GattConnectionEvent.Connecting   -> "Connecting…"
                GattConnectionEvent.Connected    -> "Discovering services…"
                GattConnectionEvent.ServicesDiscovered -> "Connected"
                GattConnectionEvent.Disconnected -> "Disconnected"
                is GattConnectionEvent.WriteCompleted -> "Connected"
                is GattConnectionEvent.CharacteristicRead -> "Connected"
                is GattConnectionEvent.Error     -> "Error: ${(state as GattConnectionEvent.Error).reason}"
                else                             -> state.toString()
            }
        )

        /* connect / disconnect */
        Button(
            enabled = !inFlight,
            onClick = {
                if (connected) vm.disconnect()
                else           nav.navigate(Screen.Scan.route)
            }
        ) {
            Icon(
                if (connected) Icons.Default.BluetoothDisabled else Icons.Default.Bluetooth,
                contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
            Text(if (connected) "Disconnect" else "Connect")
        }


        ChatTypeButton(
            label = "Broadcast",
            enabled = true
        ) {
            nav.navigate(
                Screen.Chat.route
                    .replace("{chatId}", "0")
                    .replace("{title}",  "Broadcast")
            )
        }

        ChatTypeButton(
            label   = "Node ↔ Node",
            enabled = true
        ) {
            nav.navigate(Screen.ChatList.route.replace("{type}", "NODE"))
        }

        ChatTypeButton(
            label   = "User ↔ User",
            enabled = true
        ) {
            nav.navigate(Screen.ChatList.route.replace("{type}", "USER"))
        }
    }
}


@Composable
private fun ChatTypeButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled  = enabled,
        onClick  = onClick
    ) {
        Icon(Icons.Default.Chat, null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}