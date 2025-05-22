package com.example.disastermesh.core.ble.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Transaction
import com.example.disastermesh.core.ble.GattManager
import com.example.disastermesh.core.ble.MAX_MSG_CHARS
import com.example.disastermesh.core.ble.idTarget
import com.example.disastermesh.core.ble.idType
import com.example.disastermesh.core.ble.makeChatId
import com.example.disastermesh.core.crypto.CryptoBox
import com.example.disastermesh.core.data.*
import com.example.disastermesh.core.database.MessageType
import com.example.disastermesh.core.database.dao.ChatDao
import com.example.disastermesh.core.database.dao.PublicKeyDao
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.entities.MessageStatus
import com.example.disastermesh.core.database.entities.PublicKey
import com.example.disastermesh.core.database.entities.Route
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Singleton
class BleMeshRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val gatt: GattManager,
    private val dao: ChatDao,
    private val pkDao: PublicKeyDao
) : MeshRepository {

    private val _nodeList = MutableSharedFlow<List<Int>>(replay = 0)
    private val _userList = MutableSharedFlow<List<Int>>(replay = 0)

    override val nodeList: Flow<List<Int>> = _nodeList
    override val userList: Flow<List<Int>> = _userList

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _gw = MutableStateFlow(false)
    override val gatewayAvailable: StateFlow<Boolean> = _gw

    private val _routeUpdates = MutableSharedFlow<Pair<Long, Route>>(extraBufferCapacity = 16)
    override val routeUpdates: SharedFlow<Pair<Long, Route>> = _routeUpdates   // VM listens


    init {
        gatt.incomingMessages()
            .mapNotNull { MessageCodec.decode(it) }
            .onEach { obj ->

                when (obj) {
                    is PubKeyResp -> pkDao.upsert(PublicKey(obj.userId, obj.key))
                    is AckSuccess -> dao.setStatusByPktId(obj.pktId.toInt(), MessageStatus.ACKED)
                    is GatewayAvailable -> _gw.value = true
                    is GatewayChatMessage -> persist(obj.inner, Route.GATEWAY)
                    is ChatMessage -> persist(obj, Route.MESH)
                    is ListResponse -> when (obj.opcode) {
                        Opcode.LIST_NODES_RESP -> _nodeList.emit(obj.ids)
                        Opcode.LIST_USERS_RESP -> _userList.emit(obj.ids)
                        else -> {}
                    }
                    is EncChatMessage -> handleEnc(obj)

                }
            }
            .launchIn(scope)
    }

    /* ---------------- public API ---------------------- */

    override fun chats(type: MessageType) = dao.chatsOfType(type)

    override fun stream(chatId: Long) = dao.messages(chatId)

    /* ---------- key helpers --------------------------------------- */

    override fun publicKeyFlow(userId: Int) = pkDao.keyFlow(userId)

    override suspend fun requestPublicKey(userId: Int) {
        gatt.sendMessage(MessageCodec.encodeRequestPubKey(userId))
    }

    /* ---------- encryption flag helpers --------------------------- */
    override fun encryptedFlow(chatId: Long) = dao.encryptedFlow(chatId)

    override suspend fun setEncrypted(chatId: Long, on: Boolean) =
        dao.setEncrypted(chatId, on)


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Transaction
    override suspend fun send(chatId: Long, body: String, myUserId: Int) {
        require(body.length <= MAX_MSG_CHARS) {
            "Message too long (${body.length} > $MAX_MSG_CHARS)"
        }
        val type = idType(chatId)
        val target = idTarget(chatId)
        val encOn = dao.encryptedFlow(chatId).first() && type == MessageType.USER
        val pktId = (System.currentTimeMillis() and 0xFFFF_FFFFL).toUInt()

        val ok = if (encOn) {
            /* lookup keys */
            val theirPk: ByteArray = pkDao.key(target) ?: return

            val cipher = CryptoBox.encrypt(ctx, body, myUserId, theirPk)
            val bytes  = MessageCodec.encodeEncUserMsg(pktId, target, myUserId, cipher)
            gatt.sendMessage(bytes)
        } else {
            val cm = when (type) {
                MessageType.BROADCAST -> ChatMessage(pktId, type, 0, 0, body)
                MessageType.NODE -> ChatMessage(pktId, type, target, 0, body)
                MessageType.USER -> ChatMessage(pktId, type, target, myUserId, body)
            }
            gatt.sendMessage(MessageCodec.encode(cm))
        }.let { kotlin.runCatching { it }.isSuccess }

        val title = when (type) {
            MessageType.BROADCAST -> "Broadcast"
            MessageType.NODE -> "Node $target"
            MessageType.USER -> "User $target"
        }
        dao.upsertChat(Chat(id = chatId, type = type, title = title, encrypted = encOn))

        /* optimistic local insert */
        val msgId = dao.insert(
            Message(
                chatId = chatId,
                mine = true,
                body = body,
                pktId = pktId.toInt(),
                status = MessageStatus.SENDING
            )
        )

        dao.setStatus(msgId, if (ok) MessageStatus.SENT else MessageStatus.SENDING)

    }

    @Transaction
    override suspend fun sendGateway(chatId: Long, body: String, myUserId: Int) {
        val type = idType(chatId)
        val target = idTarget(chatId)
        require(type == MessageType.USER) { "gateway route only for user chats" }
        val pktId = (System.currentTimeMillis() and 0xFFFF_FFFFL).toUInt()
        val cm = ChatMessage(pktId, type, target, myUserId, body)
        val msgId = dao.insert(
            Message(
                chatId = chatId,
                mine = true,
                body = body,
                pktId = pktId.toInt(),
                status = MessageStatus.SENDING
            )
        )

        val ok = runCatching { gatt.sendMessage(MessageCodec.encodeGateway(cm)) }.isSuccess
        dao.setStatus(msgId, if (ok) MessageStatus.SENT else MessageStatus.SENDING)
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

//    private suspend fun saveIncoming(cm: ChatMessage) {
//        val cid = makeChatId(cm.type, cm.sender)
//        dao.upsertChat(
//            Chat(
//                id    = cid,
//                type  = cm.type,
//                title = when (cm.type) {
//                    MessageType.BROADCAST -> "Broadcast"
//                    MessageType.NODE      -> "Node ${cm.sender}"
//                    MessageType.USER      -> "User ${cm.sender}"
//                }
//            )
//        )
//        dao.insert(Message(chatId = cid, mine = false, body = cm.body))
//    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun handleEnc(enc: EncChatMessage) {
        /* my private key ↔ sender’s public key */
        println("handleEnc")
        val myUid     = enc.dest          // because dest = me
        val senderUid = enc.sender
        val senderPk  = pkDao.key(senderUid) ?: return   // cannot decrypt

        val plain = runCatching {
            CryptoBox.decrypt(ctx, enc.cipher, myUid, senderPk)
        }.getOrElse { println("decrypt failed: $it"); return }            // auth failed → drop silently
        println("decrypted: $plain")

        val cm = ChatMessage(
            pktId  = enc.pktId,
            type   = MessageType.USER,
            dest   = myUid,
            sender = senderUid,
            body   = plain
        )
        persist(cm, Route.MESH)           // reuse existing helper
    }


    private suspend fun persist(cm: ChatMessage, newRoute: Route) {
        val cid = makeChatId(cm.type, cm.sender)

        dao.upsertChat(
            Chat(
                id = cid,
                type = cm.type,
                title = when (cm.type) {
                    MessageType.BROADCAST -> "Broadcast"
                    MessageType.NODE -> "Node ${cm.sender}"
                    MessageType.USER -> "User ${cm.sender}"
                },
                route = newRoute
            )
        )

        val prev = dao.getRoute(cid)
        if (prev != newRoute) {
            dao.setRoute(cid, newRoute)
            if (newRoute == Route.GATEWAY) _routeUpdates.emit(cid to newRoute)
        }


        dao.insert(Message(chatId = cid, mine = false, body = cm.body, pktId = cm.pktId.toInt()))
    }

    override suspend fun setRoute(cid: Long, r: Route) {
        dao.setRoute(cid, r)
    }

    override suspend fun getRoute(cid: Long): Route? = dao.getRoute(cid)


}
