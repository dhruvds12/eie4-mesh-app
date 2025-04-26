package com.example.disastermesh.core.ble.repository

import com.example.disastermesh.core.database.MessageType
import kotlinx.coroutines.flow.Flow

interface MeshRepository {

    /* ------- high-level chat API (UI uses only these) ------------------ */

    fun chats(type: MessageType): Flow<List<com.example.disastermesh.core.database.entities.Chat>>

    fun stream(chatId: Long): Flow<List<com.example.disastermesh.core.database.entities.Message>>

    suspend fun send(chatId: Long, body: String, myUserId: Int)
}
