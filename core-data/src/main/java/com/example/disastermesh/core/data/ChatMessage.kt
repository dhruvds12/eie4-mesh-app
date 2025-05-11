package com.example.disastermesh.core.data

import com.example.disastermesh.core.database.MessageType

/**
 * When type ==
 *
 * Broadcast -> type = 1 ignore remaining fields
 * Node -> type = 2
 *  On send: dest is the dest node id and sender is ignored
 *  On receive: dest is my nodes name and sender is the sender node id
 *
 * User -> type = 3
 *  On send: dest is the user id and sender is my ID
 *  On receive: dest is my ID and sender is the user id
 */
data class ChatMessage(
    val pktId: UInt,
    val type: MessageType,
    val dest: Int,  // could a user or a node -> check type
    val sender: Int, // could be a user or a node -> check type
    val body:  String
)