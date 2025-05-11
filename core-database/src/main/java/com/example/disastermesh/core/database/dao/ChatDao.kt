package com.example.disastermesh.core.database.dao


import androidx.room.*
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.MessageType
import com.example.disastermesh.core.database.entities.MessageStatus
import com.example.disastermesh.core.database.entities.Route
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    /* ---------- chat list ------------------------------------------------ */

    @Query("SELECT * FROM Chat WHERE type = :t ORDER BY title")
    fun chatsOfType(t: MessageType): Flow<List<Chat>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertChat(chat: Chat)

    /* ---------- messages ------------------------------------------------- */

    @Query("SELECT * FROM Message WHERE chatId = :cid ORDER BY ts")
    fun messages(cid: Long): Flow<List<Message>>

    @Insert
    suspend fun insert(msg: Message): Long

    @Query("UPDATE Message SET status = :s WHERE msgId = :id")
    suspend fun setStatus(id: Long, s: MessageStatus)

    @Query("UPDATE Chat SET route = :r WHERE id = :cid")
    suspend fun setRoute(cid: Long, r: Route)

    @Query("SELECT route FROM Chat WHERE id = :cid")
    suspend fun getRoute(cid: Long): Route?

    @Query("UPDATE Message SET status = :s WHERE pktId = :pid")
    suspend fun setStatusByPktId(pid: Int, s: MessageStatus)
}
