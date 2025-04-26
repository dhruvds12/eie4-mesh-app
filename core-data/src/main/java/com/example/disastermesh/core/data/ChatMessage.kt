package com.example.disastermesh.core.data

import com.example.disastermesh.core.database.MessageType

data class ChatMessage(
    val type: MessageType,
    val destA: Int,
    val destB: Int,
    val body:  String
)