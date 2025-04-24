package com.example.disastermesh.feature.ble

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BleScanScreen(
    modifier: Modifier = Modifier,
    viewModel: BleScanViewModel = hiltViewModel()
) {
    // 1) Request runtime permissions:
    val permissions = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val permState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        permState.launchMultiplePermissionRequest()
    }

    // 2) Show a message if we’re missing anything:
    if (!permState.allPermissionsGranted) {
        Text(
            text = "Please grant BLE & location permissions to start scanning.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    // 3) Once granted, collect list of devices:
    val devices by viewModel.devices.collectAsState()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(devices) { device ->
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = device.name ?: "Unknown device",
                    fontWeight = FontWeight.Bold
                )
                Text(text = device.address)
                Text(text = "RSSI: ${device.rssi}")
            }
            HorizontalDivider()
        }
    }
}
