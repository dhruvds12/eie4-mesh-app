package com.example.disastermesh.core.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface GattManager {
    fun connect(address: String): Flow<GattConnectionEvent>

    fun disconnect()

    /**
     * Send a byte-array message to the node.
     * Suspends until the write has been acknowledged (or times out).
     */
    suspend fun sendMessage(payload: ByteArray)

    /**
     * A cold Flow of incoming raw payloads.
     * Each notification from RX characteristic gets emitted here.
     */
    fun incomingMessages(): Flow<ByteArray>

    fun connectionEvents(): StateFlow<GattConnectionEvent?>
}

sealed class GattConnectionEvent {
    data object Connecting : GattConnectionEvent()
    data object  Connected : GattConnectionEvent()
    data object  Disconnected : GattConnectionEvent()
    data class WriteCompleted  (val uuid: UUID)                           : GattConnectionEvent()
    data object ServicesDiscovered : GattConnectionEvent()
    data class CharacteristicRead(val uuid: UUID, val value: ByteArray) : GattConnectionEvent()
    data class Error(val reason: String) : GattConnectionEvent()
}