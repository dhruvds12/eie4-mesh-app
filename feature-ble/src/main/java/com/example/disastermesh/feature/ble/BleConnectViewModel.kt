package com.example.disastermesh.feature.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BleConnectViewModel @Inject constructor(
    private val gattManager: GattManager
) : ViewModel() {

    private val _connectionState =
        MutableStateFlow<GattConnectionEvent>(GattConnectionEvent.Disconnected)
    val connectionState: StateFlow<GattConnectionEvent> =
        _connectionState.asStateFlow()

    fun connect(address: String) {
        viewModelScope.launch {
            gattManager.connect(address)
                .collect { event ->
                    _connectionState.value = event
                }
        }
    }

    fun disconnect() {
        gattManager.disconnect()
    }
}
