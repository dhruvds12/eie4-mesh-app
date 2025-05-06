// core‑net/UserNetRepository.kt
package com.example.disastermesh.core.net

import com.example.disastermesh.core.database.dao.ChatDao
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.entities.MessageStatus
import com.example.disastermesh.core.ble.makeChatId
import com.example.disastermesh.core.database.MessageType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest   //  ← NEW
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserNetRepository @Inject constructor(
    private val api: MessageApi,
    private val dao: ChatDao,
    private val conn: ConnectivityObserver
) {

    private var pollJob: Job? = null

    /* ---------- outbound --------------------------------------------- */

    suspend fun send(myUid: Int, targetUid: Int, chatId: Long, body: String) {
        val localId = dao.insert(Message(chatId = chatId, mine = true, body = body))
        val msg     = NetMessage(src = "$myUid", dst = "$targetUid", body = body)

        api.post(msg).let { rsp ->
            if (rsp.isSuccessful) dao.setStatus(localId, MessageStatus.SENT)
        }
    }

    /* ---------- inbound polling -------------------------------------- */

    fun start(uid: Int, scope: CoroutineScope) {
        if (pollJob?.isActive == true) return       // already running

        pollJob = scope.launch {
            conn.isOnline
                .distinctUntilChanged()
                .collectLatest { online ->
                    if (!online) return@collectLatest          // wait for next ‘true’

                    var since = 0L                             // Unix seconds
                    while (isActive) {
                        val resp = runCatching {
                            api.sync(UserSyncReq("$uid", since))
                        }.getOrNull()

                        resp?.body()?.let { sync ->
                            insertDown(sync.down)
                            since = System.currentTimeMillis() / 1000
                        }
                        delay(5_000)
                    }
                }
        }
    }

    private suspend fun insertDown(msgs: List<NetMessage>) {
        for (m in msgs) {
            val src = m.src.toInt()
            val cid = makeChatId(MessageType.USER, src)

            dao.upsertChat(Chat(id = cid, type = MessageType.USER, title = "User $src"))
            dao.insert(
                Message(
                    chatId = cid,
                    mine   = false,
                    body   = m.body,
                    ts     = m.ts * 1000   // server sends seconds → ms
                )
            )
        }
    }
}
