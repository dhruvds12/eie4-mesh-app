package com.example.disastermesh.feature.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import com.example.disastermesh.core.ble.MeshRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BleConnectViewModel @Inject constructor(
    private val gatt: GattManager,
    private val repo: MeshRepository
) : ViewModel() {

    /** Connection status from the GATT layer */
    private val _state = MutableStateFlow<GattConnectionEvent?>(null)
    val  connectionState: StateFlow<GattConnectionEvent?> = _state.asStateFlow()

    /** One growing list that contains everything shown in the chat */
    data class ChatMessage(val text: String, val fromMe: Boolean)
    private val _chat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val         chat : StateFlow<List<ChatMessage>> = _chat.asStateFlow()

    /** --------------------------------------------------------------------- */

    fun connect(address: String) {
        /* 1) connection-state updates */
        gatt.connect(address)
            .collectIn(viewModelScope) { evt -> _state.value = evt }

        /* 2) messages coming from the node */
        viewModelScope.launch {
            repo.incomingText().collect { incoming ->
                _chat.update { it + ChatMessage(incoming, fromMe = false) }
            }
        }
    }

    fun disconnect() {
        gatt.disconnect()
    }

    fun send(text: String) {
        if (text.isBlank()) return                      // ignore empty sends

        /* optimistic append – shows up immediately */
        _chat.update { it + ChatMessage(text, fromMe = true) }

        viewModelScope.launch { repo.sendText(text) }
    }

    /* helper: collect a Flow inside a scope with ergonomic syntax */
    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectIn(
        scope: kotlinx.coroutines.CoroutineScope,
        crossinline block: suspend (T) -> Unit
    ) = scope.launch { collect { block(it) } }
}
