package com.example.disastermesh.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.disastermesh.core.database.MessageType

/**
 * One chat “room”.
 *
 * id rules
 * • 0                     → Broadcast
 * • 1 …  9 999            → Node-to-Node   (nodeId = id)
 * • 10 000 … Long.MAX     → User-to-User  (userId = id)
 */
@Entity
data class Chat(
    @PrimaryKey val id: Long,
    val type : MessageType,
    val title: String
)
