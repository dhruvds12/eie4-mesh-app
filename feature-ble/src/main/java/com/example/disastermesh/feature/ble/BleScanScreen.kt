package com.example.disastermesh.feature.ble

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.feature.ble.ui.LandingViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState


@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BleScanScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    /* ------------------------------------------------------------------ */
    /*  Step-1: request permissions                                       */
    /* ------------------------------------------------------------------ */
    val permState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(Unit) { permState.launchMultiplePermissionRequest() }

    if (!permState.allPermissionsGranted) {
        /* show message while waiting or if denied */
        Text(
            "Please grant BLE & location permissions to start scanning.",
            modifier = Modifier.padding(16.dp)
        )
        return                     //  ←  do **not** build the rest of the UI yet
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    /* Landing-level VM (shared) */
    val landingEntry = remember(currentEntry) {
        navController.getBackStackEntry(Screen.Landing.route)
    }
    val landingVm: LandingViewModel = hiltViewModel(landingEntry)

    /* Scan-specific VM */
    val scanVm: BleScanViewModel = hiltViewModel()

    val devices by scanVm.devices.collectAsState(emptyList())

    LazyColumn(modifier = modifier) {
        items(devices) { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        landingVm.connect(device.address)   // start GATT connect
                        navController.popBackStack()        // back to Landing
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(device.name ?: "Unknown", fontWeight = FontWeight.Bold)
                Text(device.address)
                Text("RSSI: ${device.rssi}")
            }
            HorizontalDivider()
        }
    }
}

