package com.example.disastermesh.feature.ble.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.disastermesh.core.ble.makeChatId          // ← NEW
import com.example.disastermesh.core.database.MessageType
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.feature.ble.ui.model.NewUserChatDialog
import kotlinx.coroutines.launch

@Composable
fun DiscoveryScreen(
    type : DiscoveryType,          // NODE or USER
    nav  : NavController,
    vm   : DiscoveryVm = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }

    NewUserChatDialog(                    // reuse the component
        show = showDialog,
        onDismiss = { showDialog = false },
        onCreate  = { phone ->
            showDialog = false
            scope.launch {
                val uid   = com.example.disastermesh.core.ble.phoneToUserId(phone)
                val cid   = com.example.disastermesh.core.ble.makeChatId(
                    MessageType.USER, uid
                )
                vm.ensureChat(MessageType.USER, uid, "User $uid")
                nav.navigate(
                    Screen.Chat.route
                        .replace("{chatId}", cid.toString())
                        .replace("{title}",  "User $uid")
                )
            }
        }
    )

    /* start query once */
    LaunchedEffect(Unit) { vm.query(type) }

    val ids   by vm.ids.collectAsState()
    val empty by vm.empty.collectAsState(false)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("Known ${type.label}s")

        when {
            ids == null && !empty -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(8.dp))
                    Text("Searching …")
                }
            }

            empty -> Text("No ${type.label.lowercase()}s found.")

            else -> LazyColumn {
                items(ids ?: emptyList(), key = { it }) { id ->
                    val msgType = if (type == DiscoveryType.NODE)
                        MessageType.NODE else MessageType.USER
                    val title   = if (type == DiscoveryType.NODE)
                        "Node $id" else "User $id"

                    Text(
                        text = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    /* 1) make sure Chat row exists */
                                    vm.ensureChat(msgType, id, title)

                                    /* 2) open the chat */
                                    val chatId = makeChatId(msgType, id)
                                    nav.navigate(
                                        Screen.Chat.route
                                            .replace("{chatId}", chatId.toString())
                                            .replace("{title}",  title)
                                    )
                                }
                            }
                            .padding(8.dp)
                    )
                }
            }
        }

        if (type == DiscoveryType.USER) {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Enter ID")
            }
        }
    }
}

/** which list we are showing */
enum class DiscoveryType(val label: String) { NODE("Node"), USER("User") }
