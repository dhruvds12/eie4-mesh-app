package com.example.disastermesh.core.ble.repository

import androidx.room.Transaction
import com.example.disastermesh.core.MAX_MSG_CHARS
import com.example.disastermesh.core.ble.GattManager
import com.example.disastermesh.core.data.*
import com.example.disastermesh.core.database.MessageType
import com.example.disastermesh.core.database.dao.ChatDao
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.entities.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleMeshRepository @Inject constructor(
    private val gatt : GattManager,
    private val dao  : ChatDao
) : MeshRepository {

    private val _nodeList = MutableSharedFlow<List<Int>>(replay = 0)
    private val _userList = MutableSharedFlow<List<Int>>(replay = 0)

    override val nodeList: Flow<List<Int>> = _nodeList
    override val userList: Flow<List<Int>> = _userList

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        gatt.incomingMessages()
            .mapNotNull { MessageCodec.decode(it) }
            .onEach  { obj ->
                when (obj) {
                    is ChatMessage   -> saveIncoming(obj)
                    is ListResponse  -> when (obj.opcode) {
                        Opcode.LIST_NODES_RESP -> _nodeList.emit(obj.ids)
                        Opcode.LIST_USERS_RESP -> _userList.emit(obj.ids)
                        else -> {}
                    }
                }
            }
            .launchIn(scope)
    }

    /* ---------------- public API ---------------------- */

    override fun chats(type: MessageType) = dao.chatsOfType(type)

    override fun stream(chatId: Long)  = dao.messages(chatId)

    @Transaction
    override suspend fun send(chatId: Long, body: String, myUserId: Int) {
        require(body.length <= MAX_MSG_CHARS) {
            "Message too long (${body.length} > $MAX_MSG_CHARS)"
        }
        val (type, a, b) = when (chatId) {
            0L            -> Triple(MessageType.BROADCAST, 0, 0)
            in   1..9_999L-> Triple(MessageType.NODE,      chatId.toInt(), 0)
            else          -> Triple(MessageType.USER,      chatId.toInt(), myUserId) // destA is destination and destB is my ID
        }
        val cm = ChatMessage(type, a, b, body)

        val title = when (type) {
            MessageType.BROADCAST -> "Broadcast"
            MessageType.NODE      -> "Node $a"
            MessageType.USER      -> "User $a"
        }
        dao.upsertChat(Chat(id = chatId, type = type, title = title))

        /* optimistic local insert */
        val msgId = dao.insert(Message(chatId = chatId, mine = true, body = body))

        val ok = runCatching {
            gatt.sendMessage(MessageCodec.encode(cm))
        }.isSuccess

        dao.setStatus(
            id = msgId,
            s  = if (ok) MessageStatus.SENT else MessageStatus.SENDING
        )

    }

    suspend fun requestNodes() = gatt.sendMessage(
        MessageCodec.encodeListRequest(Opcode.LIST_NODES_REQ)
    )

    suspend fun requestUsers() = gatt.sendMessage(
        MessageCodec.encodeListRequest(Opcode.LIST_USERS_REQ)
    )

    suspend fun ensureChatExists(id: Long, title: String, type: MessageType) {
        dao.upsertChat(Chat(id = id, title = title, type = type))
    }


    /* ---------------- helpers ------------------------- */

    private suspend fun saveIncoming(cm: ChatMessage) {
        val cid = when (cm.type) {
            MessageType.BROADCAST -> 0L
            MessageType.NODE      -> cm.sender.toLong()
            MessageType.USER      -> cm.sender.toLong()
        }

        dao.upsertChat(
            Chat(
                id    = cid,
                type  = cm.type,
                title = when (cm.type) {
                    MessageType.BROADCAST -> "Broadcast"
                    MessageType.NODE      -> "Node ${cm.sender}"
                    MessageType.USER      -> "User ${cm.sender}"
                }
            )
        )
        dao.insert(Message(chatId = cid, mine = false, body = cm.body))
    }
}
