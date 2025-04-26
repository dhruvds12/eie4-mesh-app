/* feature-ble/chat/BleChatScreen.kt */
package com.example.disastermesh.feature.ble

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.feature.ble.ui.DateHeader
import com.example.disastermesh.feature.ble.ui.MessageBubble
import com.example.disastermesh.feature.ble.ui.model.ChatItem
import com.example.disastermesh.feature.ble.ui.LandingViewModel

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
    val connected by landingVm.connection.collectAsState()

    val items by vm.items.collectAsState()

    var draft by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(chatTitle, style = MaterialTheme.typography.titleMedium)

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

        val canSend = when (connected) {
            GattConnectionEvent.ServicesDiscovered     -> true
            is GattConnectionEvent.WriteCompleted      -> true
            is GattConnectionEvent.CharacteristicRead  -> true
            else                                       -> false
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Type a message…") },
                modifier = Modifier.weight(1f),
                enabled  = canSend
            )
            Button(
                onClick  = { vm.send(draft); draft = "" },
                enabled  = canSend && draft.isNotBlank()
            ) { Text("Send") }
        }
    }
}
