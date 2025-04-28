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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.disastermesh.feature.ble.nav.Screen
import com.example.disastermesh.core.database.entities.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@Composable
fun ChatListScreen(
    type: String,
    nav : NavController,
    vm  : ChatListVm = hiltViewModel()
) {
    val chats by vm.chats(type).collectAsState(emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(
                Screen.Discover.route.replace("{kind}", if (type == "NODE") "NODE" else "USER") ) }) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            items(chats, key = Chat::id) { c ->
                ListItem(
                    headlineContent = { Text(c.title) },
                    modifier = Modifier
                        .clickable {
                            nav.navigate(
                                Screen.Chat.route
                                    .replace("{chatId}", c.id.toString())
                                    .replace("{title}", c.title)
                            )
                        }
                )
                HorizontalDivider()
            }
        }
    }
}

/* ViewModel ------------------------------------------------------------ */

@HiltViewModel
class ChatListVm @Inject constructor(
    private val repo: com.example.disastermesh.core.ble.repository.MeshRepository
) : ViewModel() {

    fun chats(type: String) = when (type) {
        "NODE" -> repo.chats(com.example.disastermesh.core.database.MessageType.NODE)
        "USER" -> repo.chats(com.example.disastermesh.core.database.MessageType.USER)
        else   -> flowOf(emptyList())
    }
}
