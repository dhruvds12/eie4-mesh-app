package com.example.disastermesh.feature.ble.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val gatt: GattManager
) : ViewModel() {

    private val _connection = MutableStateFlow<GattConnectionEvent?>(null)
    val connection: StateFlow<GattConnectionEvent?> = _connection

    private var connectJob: Job? = null          // ← track current job

    /** Called from the Scan screen when user taps a device. */
    fun connect(address: String) {
        // if already trying the same address, ignore
        if (connectJob?.isActive == true) return

        connectJob = viewModelScope.launch {
            gatt.connect(address).collect { _connection.value = it }
        }
    }

    fun disconnect() { gatt.disconnect() }
}
