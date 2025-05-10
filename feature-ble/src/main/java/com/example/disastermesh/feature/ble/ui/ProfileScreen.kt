package com.example.disastermesh.feature.ble.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.disastermesh.core.ble.phoneToUserId


@Composable
fun ProfileScreen(
    nav: NavController,
    vm : ProfileVm = hiltViewModel()
) {
    val profile by vm.profile.collectAsState()

    var name  by remember(profile) { mutableStateOf(profile?.name  ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone (+CC)") }
        )

        Button(
            onClick = {
                vm.save(name, phone)
                val uid = phoneToUserId(phone)
                println("derived userId = $uid")
                nav.popBackStack()
            },
            enabled = name.isNotBlank() && phone.startsWith("+")
        ) { Text("Save") }
    }
}

