package com.example.disastermesh.core.ble

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface GattManager {
    fun connect(address: String): Flow<GattConnectionEvent>

    fun disconnect()
}

sealed class GattConnectionEvent {
    data object Connecting : GattConnectionEvent()
    data object  Connected : GattConnectionEvent()
    data object  Disconnected : GattConnectionEvent()
    data object ServicesDiscovered : GattConnectionEvent()
    data class CharacteristicRead(val uuid: UUID, val value: ByteArray) : GattConnectionEvent()
    data class Error(val reason: String) : GattConnectionEvent()
}