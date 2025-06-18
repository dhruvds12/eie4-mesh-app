package com.example.disastermesh.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.disastermesh.core.database.MessageType

/**
 * Chat-ID layout:
 *  0L                       → Broadcast
 *  type.id in bits 32-39    → Message type   (NODE = 2, USER = 3)
 *  target  in bits  0-31    → Counter-party  (nodeId or userId)
 */

@Entity
data class Chat(
    @PrimaryKey val id: Long,
    val type : MessageType,
    val title: String,
    val route: Route = Route.MESH,
    val encrypted: Boolean = false,
    val ackRequest: Boolean = false
)
