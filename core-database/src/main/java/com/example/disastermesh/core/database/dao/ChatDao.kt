package com.example.disastermesh.core.database.dao


import androidx.room.*
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.MessageType
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
    suspend fun insert(msg: Message)
}
