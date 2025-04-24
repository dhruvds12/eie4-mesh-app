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
    address: String,
    navController: NavController,
    viewModel: BleChatViewModel = hiltViewModel()
) {
    val messages by viewModel.incomingMessages.collectAsState(initial = emptyList())
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // 1) message list
        LazyColumn(Modifier.weight(1f)) {
            items(messages) { msg ->
                Text(msg)
                Spacer(Modifier.height(4.dp))
            }
        }

        // 2) text entry + send
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
                viewModel.sendMessage(input)
                input = ""
            }) {
                Text("Send")
            }
        }
    }
}
