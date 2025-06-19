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
import com.example.disastermesh.core.data.u32
import com.example.disastermesh.core.database.MessageType
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.feature.ble.ui.model.NewUserChatDialog
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun DiscoveryScreen(
    type : DiscoveryType,          // NODE or USER
    nav  : NavController,
    vm   : DiscoveryVm = hiltViewModel()
) {
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }

    val landingEntry = remember(nav.currentBackStackEntry) {
        nav.getBackStackEntry(Screen.Landing.route)
    }
    val landingVm: LandingViewModel = hiltViewModel(landingEntry)
    val myUid   by landingVm.profile.collectAsState(null)   // profile?.uid
    val myNode  by landingVm.nodeId.collectAsState()

    NewUserChatDialog(                    // reuse the component
        show = showDialog,
        onDismiss = { showDialog = false },
        onCreate  = { phone ->
            showDialog = false
            scope.launch {
                val uid   = com.example.disastermesh.core.ble.phoneToUserId(phone)
                val cid   = makeChatId(
                    MessageType.USER, uid
                )
                vm.ensureChat(MessageType.USER, uid, "User ${uid.u32}")
                nav.navigate(
                    Screen.Chat.route
                        .replace("{chatId}", cid.toString())
                        .replace("{title}",  "User ${uid.u32}")
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

        Text(
            "Known ${type.label}s",
            style = MaterialTheme.typography.titleMedium
        )

        when {
            ids == null && !empty -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(8.dp))
                    Text("Searching …")
                }
            }

            empty -> Text("No ${type.label.lowercase()}s found.")

//            else -> LazyColumn {
//                items(ids ?: emptyList(), key = { it }) { id ->
//                    val msgType = if (type == DiscoveryType.NODE)
//                        MessageType.NODE else MessageType.USER
//                    val title   = if (type == DiscoveryType.NODE)
//                        "Node ${id.u32}" else "User ${id.u32}"
//
//                    Text(
//                        text = title,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clickable {
//                                scope.launch {
//                                    /* 1) make sure Chat row exists */
//                                    vm.ensureChat(msgType, id, title)
//
//                                    /* 2) open the chat */
//                                    val chatId = makeChatId(msgType, id)
//                                    nav.navigate(
//                                        Screen.Chat.route
//                                            .replace("{chatId}", chatId.toString())
//                                            .replace("{title}",  title)
//                                    )
//                                }
//                            }
//                            .padding(8.dp)
//                    )
//                }
//            }
            else -> {
                /* ① filter ------------------------------ */
                val visibleIds = (ids ?: emptyList()).filter { id ->
                    when (type) {
                        DiscoveryType.USER -> id != (myUid?.uid ?: -1)
                        DiscoveryType.NODE -> id != (myNode ?: -1)
                    }
                }

                /* ② empty-after-filter case -------------- */
                if (visibleIds.isEmpty()) {
                    Text("No other ${type.label.lowercase()}s found.")
                } else LazyColumn {
                    items(visibleIds, key = { it }) { id ->
                        val msgType = if (type == DiscoveryType.NODE)
                            MessageType.NODE else MessageType.USER
                        val title   = if (type == DiscoveryType.NODE)
                            "Node ${id.u32}" else "User ${id.u32}"

                        /* ③ prettier row ------------------ */
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    scope.launch {
                                        vm.ensureChat(msgType, id, title)
                                        val chatId = makeChatId(msgType, id)
                                        nav.navigate(
                                            Screen.Chat.route
                                                .replace("{chatId}", chatId.toString())
                                                .replace("{title}",  title)
                                        )
                                    }
                                }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
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
