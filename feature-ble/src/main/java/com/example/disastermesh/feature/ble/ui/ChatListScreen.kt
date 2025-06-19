package com.example.disastermesh.feature.ble.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.disastermesh.core.data.low32u
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.feature.ble.ui.model.NewUserChatDialog
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row


@Composable
fun ChatListScreen(
    type: String,
    nav: NavController,
    vm: ChatListVm = hiltViewModel()
) {
    /* --- landing VM to know connection state ------------------------ */
    val landingEntry = remember(nav.currentBackStackEntry) {
        nav.getBackStackEntry(Screen.Landing.route)
    }
    val landingVm: LandingViewModel = hiltViewModel(landingEntry)
    val ui by landingVm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    /* --- chats ------------------------------------------------------ */
    val chats by vm.chats(type).collectAsState(emptyList())

    /* --- observe “created chat” event ------------------------------- */
    LaunchedEffect(Unit) {
        vm.newChat.collect { cid ->
            nav.navigate(
                Screen.Chat.route
                    .replace("{chatId}", cid.toString())
                    .replace("{title}", "User ${cid.low32u}")
            )
        }
    }

    /* --- dialog state ---------------------------------------------- */
    var showDialog by remember { mutableStateOf(false) }
//    var phoneInput by remember { mutableStateOf("") }


    NewUserChatDialog(
        show = showDialog,
        onDismiss = { showDialog = false },
        onCreate = { phone ->
            showDialog = false
            scope.launch { vm.createUserChat(phone) }
        }
    )


    /* --- FAB logic -------------------------------------------------- */
    val isUserList = type == "USER"
    val canViaBle = ui.bleConnected
    val canViaInternet = ui.internet && !ui.bleConnected

    val fabEnabled = when {
        isUserList -> canViaBle || canViaInternet
        else -> canViaBle                       // NODE list
    }

    val onFabClick: () -> Unit = onFabClick@{
        if (!fabEnabled) return@onFabClick
        if (isUserList && !canViaBle) {
            showDialog = true                         // internet path
        } else {
            nav.navigate(
                Screen.Discover.route.replace(
                    "{kind}", if (isUserList) "USER" else "NODE"
                )
            )
        }
    }

    /* --- UI --------------------------------------------------------- */
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
            ) { Icon(Icons.Default.Add, contentDescription = "New chat") }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {


            item {
                val title = if (type == "USER") "user-to-user" else "node-to-node"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    IconButton(
                        onClick = { nav.navigateUp() },
                        modifier = Modifier.size(40.dp)            // keeps ≥48 dp touch-target
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        "Recent $title chats",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            items(chats, key = Chat::id) { c ->
//                ListItem(
//                    headlineContent = { Text(c.title) },
//                    modifier = Modifier.clickable {
//                        nav.navigate(
//                            Screen.Chat.route
//                                .replace("{chatId}", c.id.toString())
//                                .replace("{title}", c.title)
//                        )
//                    }
//                )

                Card(
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable {
                            nav.navigate(
                                Screen.Chat.route
                                    .replace("{chatId}", c.id.toString())
                                    .replace("{title}", c.title)
                            )
                        }
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                c.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
//                HorizontalDivider()
            }
        }
    }
}


