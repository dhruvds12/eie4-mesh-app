package com.example.disastermesh.feature.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.disastermesh.core.ble.GattConnectionEvent
import com.example.disastermesh.core.ble.GattManager
import com.example.disastermesh.core.ble.repository.MeshRepository
import com.example.disastermesh.core.database.entities.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BleConnectViewModel @Inject constructor(
    private val gatt : GattManager,
    private val repo : MeshRepository
) : ViewModel() {

    /* ---------- connection status ------------------------------------- */
    private val _state = MutableStateFlow<GattConnectionEvent?>(null)
    val    connectionState: StateFlow<GattConnectionEvent?> = _state.asStateFlow()

    /* ---------- simple in-memory chat log shown on the screen --------- */
    data class UiLine(val text: String, val fromMe: Boolean)
    private val _chat = MutableStateFlow<List<UiLine>>(emptyList())
    val         chat : StateFlow<List<UiLine>> = _chat.asStateFlow()

    /* choose which chat the connect screen shows – broadcast for now */
    private companion object {
        const val CHAT_ID_BROADCAST = 0L
        const val MY_USER_ID        = 0        // replace when you have login
    }

    /* ------------------------------------------------------------------ */

    fun connect(address: String) {
        /* 1) connection-state updates */
        gatt.connect(address)
            .collectIn(viewModelScope) { evt -> _state.value = evt }

        /* 2) observe messages from the chosen chat */
        viewModelScope.launch {
            repo.stream(CHAT_ID_BROADCAST)
                .collect { list: List<Message> ->
                    _chat.value = list.map { UiLine(it.body, it.mine) }
                }
        }
    }

    fun disconnect() = gatt.disconnect()

    fun send(text: String) {
        if (text.isBlank()) return
        /* optimistic append */
        _chat.update { it + UiLine(text, fromMe = true) }

        viewModelScope.launch {
            repo.send(CHAT_ID_BROADCAST, text, MY_USER_ID)
        }
    }

    /* helper to collect a Flow inside a scope */
    private inline fun <T> kotlinx.coroutines.flow.Flow<T>.collectIn(
        scope: kotlinx.coroutines.CoroutineScope,
        crossinline block: suspend (T) -> Unit
    ) = scope.launch { collect { block(it) } }
}
