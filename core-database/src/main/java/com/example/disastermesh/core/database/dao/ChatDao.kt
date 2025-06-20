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
    suspend fun setStatusByPktId(pid: Long, s: MessageStatus)

    @Query("UPDATE Chat SET encrypted = :flag WHERE id = :cid")
    suspend fun setEncrypted(cid: Long, flag: Boolean)

    @Query("SELECT encrypted FROM Chat WHERE id = :cid")
    fun encryptedFlow(cid: Long): Flow<Boolean>

    @Query("SELECT 1 FROM Message WHERE pktId = :pid LIMIT 1")
    suspend fun exists(pid: Long): Boolean

    @Query("UPDATE Chat SET ackRequest = :flag WHERE id = :cid")
    suspend fun setAck(cid: Long, flag: Boolean)

    @Query("SELECT ackRequest FROM Chat WHERE id = :cid")
    fun ackFlow(cid: Long): Flow<Boolean>

    @Query("UPDATE Chat SET title = :newTitle WHERE id = :cid")
    suspend fun renameChat(cid: Long, newTitle: String)

    @Query("SELECT title FROM Chat WHERE id = :cid LIMIT 1")
    fun titleFlow(cid: Long): Flow<String>
}
