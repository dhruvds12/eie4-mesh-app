package com.example.disastermesh.feature.ble

import android.annotation.SuppressLint
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.disastermesh.feature.ble.ui.LandingViewModel



@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BleScanScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    // observe the controller’s current entry as State<NavBackStackEntry?>
    val currentEntry by navController.currentBackStackEntryAsState()

    // now remember the “landing” entry *keyed* on that currentEntry
    val landingEntry = remember(currentEntry) {
        navController.getBackStackEntry("landing")
    }
    val landingVm: LandingViewModel = hiltViewModel(landingEntry)

    val scanVm: BleScanViewModel = hiltViewModel()
    val devices by scanVm.devices.collectAsState(emptyList())

    LazyColumn(modifier = modifier) {
        items(devices) { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        landingVm.connect(device.address)
                        navController.popBackStack()
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

