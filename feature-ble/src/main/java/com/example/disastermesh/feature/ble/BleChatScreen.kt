/* feature-ble/chat/BleChatScreen.kt */
package com.example.disastermesh.feature.ble

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.database.entities.Route
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.feature.ble.ui.DateHeader
import com.example.disastermesh.feature.ble.ui.MessageBubble
import com.example.disastermesh.feature.ble.ui.model.ChatItem
import com.example.disastermesh.feature.ble.ui.LandingViewModel
import com.example.disastermesh.feature.ble.ui.MessageBar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BleChatScreen(
    chatId    : Long,
    chatTitle : String,
    navController: NavController,
    vm : BleChatViewModel = hiltViewModel()
) {
    /* make VM switch to this chat */
    LaunchedEffect(chatId) { vm.setChat(chatId) }

    /* obtain LandingViewModel (to read connection status) */
    val landingEntry = remember(navController.currentBackStackEntryAsState().value) {
        navController.getBackStackEntry(Screen.Landing.route)
    }
    val landingVm: LandingViewModel = hiltViewModel(landingEntry)
    val bleEvt     by landingVm.connection.collectAsState()
    val uiState    by landingVm.ui.collectAsState()

    val bleReady = when (bleEvt) {
        GattConnectionEvent.ServicesDiscovered,
        is GattConnectionEvent.WriteCompleted,
        is GattConnectionEvent.CharacteristicRead -> true
        else -> false
    }

    val isUserChat = remember(chatId) {            // low 8 bits = type
        com.example.disastermesh.core.ble.idType(chatId) ==
                com.example.disastermesh.core.database.MessageType.USER
    }

    val canSend = if (isUserChat) {
        bleReady || uiState.internet               // internet OR BLE
    } else {
        bleReady                                   // broadcast / node
    }


    val items by vm.items.collectAsState()

    var draft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(chatTitle, style = MaterialTheme.typography.titleMedium)

            val route by vm.currentRoute.collectAsState()
            val gwAvail by remember { vm.bleRepo.gatewayAvailable}.collectAsState()

            if (isUserChat && gwAvail) {   // && gwAvail
                var expanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { expanded = true },
                        label   = { Text(if (route == Route.MESH) "Mesh" else "Gateway") }
                    )
                    DropdownMenu(expanded, { expanded = false }) {
                        DropdownMenuItem(text = { Text("Mesh") },
                            onClick = { expanded = false; vm.setRoute(Route.MESH) })
                        DropdownMenuItem(text = { Text("Gateway") },
                            onClick = { expanded = false; vm.setRoute(Route.GATEWAY) })
                    }
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            reverseLayout = false
        ) {
            items(items, key = {
                when (it) {
                    is ChatItem.Header -> "H:${it.label}"
                    is ChatItem.Bubble -> "M:${it.msg.msgId}"
                }
            }) { item ->
                when (item) {
                    is ChatItem.Header -> DateHeader(item.label)
                    is ChatItem.Bubble -> MessageBubble(item.msg)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MessageBar(
                text        = draft,
                onTextChange= { draft = it },
                onSend      = { vm.send(draft); draft = "" },
                enabled     = canSend
            )

        }
    }
}
