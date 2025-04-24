package com.example.disastermesh.feature.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import com.example.disastermesh.core.ble.MeshRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class BleConnectViewModel @Inject constructor(
    private val gatt: GattManager,
    private val repo: MeshRepository
) : ViewModel() {

    private val _state = MutableStateFlow<GattConnectionEvent?>(null)
    val connectionState: StateFlow<GattConnectionEvent?> = _state

    private val _incoming = MutableSharedFlow<String>()
    val incomingText: SharedFlow<String> = _incoming

    fun connect(address: String) {
        // 1) collect the GATT events
        gatt.connect(address)
            .onEach { evt -> _state.value = evt }
            .launchIn(viewModelScope)

        // 2) collect incoming messages
        viewModelScope.launch {
            repo.incomingText()
                .collect { _incoming.emit(it) }
        }
    }

    fun disconnect() {
        gatt.disconnect()
    }

    fun send(text: String) {
        viewModelScope.launch {
            repo.sendText(text)
        }
    }
}

