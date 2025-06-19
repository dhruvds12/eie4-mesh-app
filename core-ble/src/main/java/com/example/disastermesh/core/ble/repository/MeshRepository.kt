package com.example.disastermesh.core.ble.repository

import com.example.disastermesh.core.database.MessageType
import com.example.disastermesh.core.database.entities.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MeshRepository {

    /* ------- high-level chat API (UI uses only these) ------------------ */

    fun chats(type: MessageType): Flow<List<com.example.disastermesh.core.database.entities.Chat>>

    fun stream(chatId: Long): Flow<List<com.example.disastermesh.core.database.entities.Message>>

    val routeUpdates: SharedFlow<Pair<Long, Route>>
    val gatewayAvailable: StateFlow<Boolean>
    val nodeList: Flow<List<Int>>
    val userList: Flow<List<Int>>

    suspend fun send(chatId: Long, body: String, myUserId: Int)
    suspend fun sendGateway(chatId: Long, body: String, myUserId: Int)
    suspend fun setRoute(cid: Long, r: Route)
    suspend fun getRoute(cid: Long): Route?

    /* -------- new key-management API ------------------------------- */
    fun publicKeyFlow(userId: Int): Flow<ByteArray?>
    suspend fun requestPublicKey(targetUid: Int, myUserId: Int)

    /* -------- per-chat encryption flag ----------------------------- */
    fun encryptedFlow(chatId: Long): Flow<Boolean>
    suspend fun setEncrypted(chatId: Long, on: Boolean)

    /* -------- per-chat ACK flag ----------------------------- */
    fun ackFlow(chatId: Long): Flow<Boolean>
    suspend fun setAck(chatId: Long, on: Boolean)

    suspend fun renameChat(cid: Long, newTitle: String)
    fun titleFlow(cid: Long): Flow<String>


}
