package com.example.disastermesh.feature.ble.ui


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.core.database.entities.Chat
import kotlinx.coroutines.launch


@Composable
fun ChatListScreen(
    type: String,
    nav : NavController,
    vm  : ChatListVm = hiltViewModel()
) {
    /* --- landing VM to know connection state ------------------------ */
    val landingEntry = remember(nav.currentBackStackEntry) {
        nav.getBackStackEntry(Screen.Landing.route)
    }
    val landingVm: LandingViewModel = hiltViewModel(landingEntry)
    val ui      by landingVm.ui.collectAsState()
    val scope   = rememberCoroutineScope()

    /* --- chats ------------------------------------------------------ */
    val chats by vm.chats(type).collectAsState(emptyList())

    /* --- observe “created chat” event ------------------------------- */
    LaunchedEffect(Unit) {
        vm.newChat.collect { cid ->
            nav.navigate(
                Screen.Chat.route
                    .replace("{chatId}", cid.toString())
                    .replace("{title}", "User ${(cid and 0xFFFF_FFFF).toInt()}")
            )
        }
    }

    /* --- dialog state ---------------------------------------------- */
    var showDialog by remember { mutableStateOf(false) }
    var phoneInput by remember { mutableStateOf("") }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDialog = false
                        scope.launch { vm.createUserChat(phoneInput) }
                    },
                    enabled = phoneInput.startsWith("+") && phoneInput.length >= 4
                ) { Text("Create") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDialog = false }
                ) { Text("Cancel") }
            },
            title = { Text("New user chat") },
            text  = {
                androidx.compose.material3.OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone (+CC)") }
                )
            }
        )
    }

    /* --- FAB logic -------------------------------------------------- */
    val isUserList     = type == "USER"
    val canViaBle      = ui.bleConnected
    val canViaInternet = ui.internet && !ui.bleConnected

    val fabEnabled = when {
        isUserList  -> canViaBle || canViaInternet
        else        -> canViaBle                       // NODE list
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
                onClick  = onFabClick,
            ) { Icon(Icons.Default.Add, contentDescription = "New chat") }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(chats, key = Chat::id) { c ->
                ListItem(
                    headlineContent = { Text(c.title) },
                    modifier = Modifier.clickable {
                        nav.navigate(
                            Screen.Chat.route
                                .replace("{chatId}", c.id.toString())
                                .replace("{title}",  c.title)
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}


