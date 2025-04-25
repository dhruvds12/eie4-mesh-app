package com.example.disastermesh.core.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface MeshRepository {
    /** Send a UTF-8 text message into the mesh. */
    suspend fun sendText(text: String)

    /** A cold Flow of UTF-8 messages received from the mesh. */
    fun incomingText(): Flow<String>
}

@Singleton
class BleMeshRepository @Inject constructor(
    private val gatt: GattManager
) : MeshRepository {
    override suspend fun sendText(text: String) {
        gatt.sendMessage(text.toByteArray(Charsets.UTF_8))
    }
    override fun incomingText(): Flow<String> =
        gatt.incomingMessages().map { bytes ->
            runCatching { String(bytes, Charsets.UTF_8) }.getOrElse {
                bytes.joinToString(" ") { "%02X".format(it) }
            }
        }

}
