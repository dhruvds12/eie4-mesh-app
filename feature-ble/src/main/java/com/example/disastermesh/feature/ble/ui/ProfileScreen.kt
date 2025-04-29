package com.example.disastermesh.feature.ble.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.disastermesh.core.ble.ProfilePrefs
import com.example.disastermesh.core.ble.phoneToUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

/* ---------------- VM ----------------------------------------------- */
@HiltViewModel
class ProfileVm @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    val profile = ProfilePrefs.flow(ctx)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(n: String, ph: String) = viewModelScope.launch {
        ProfilePrefs.set(ctx, n.trim(), ph.trim())
    }
}
