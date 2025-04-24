package com.example.disastermesh.feature.ble


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.disastermesh.core.ble.GattConnectionEvent


@OptIn(ExperimentalStdlibApi::class)
@Composable
fun BleConnectScreen(
    address: String,
    viewModel: BleConnectViewModel = hiltViewModel()
) {
    val event by viewModel.connectionState.collectAsState()

    LaunchedEffect(address) {
        viewModel.connect(address)
    }

    Column(Modifier.padding(16.dp)) {
        Text("Connecting to $address…")
        when (val e = event) {
            is GattConnectionEvent.Connecting     -> Text("Connecting…")
            is GattConnectionEvent.Connected      -> Text("Connected!")
            is GattConnectionEvent.Disconnected   -> Text("Disconnected.")
            is GattConnectionEvent.ServicesDiscovered -> Text("Services discovered.")
            is GattConnectionEvent.CharacteristicRead ->
                Text("Read ${e.uuid}: ${e.value.toHexString()}")
            is GattConnectionEvent.Error          -> Text("Error: ${e.reason}")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.disconnect() }) { Text("Disconnect") }
    }
}