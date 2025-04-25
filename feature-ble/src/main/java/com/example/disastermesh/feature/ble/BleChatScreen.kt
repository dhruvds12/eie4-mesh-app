package com.example.disastermesh.feature.ble


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun BleChatScreen(
    chatId       : Long,
    chatTitle    : String,
    navController: NavController,
    viewModel    : BleChatViewModel = hiltViewModel()
) {
    /* tell VM which chat to observe ---------------------------------- */
    LaunchedEffect(chatId) { viewModel.setChat(chatId) }

    val messages by viewModel.messages.collectAsState(emptyList())

    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(chatTitle)                       /* simple header */

        LazyColumn(Modifier.weight(1f)) {
            items(
                items = messages,
                key   = { it.msgId }
            ) { m ->
                Text(m.body)
                Spacer(Modifier.height(4.dp))
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…") }
            )
            Button(onClick = {
                viewModel.send(input)
                input = ""
            }) { Text("Send") }
        }
    }
}

