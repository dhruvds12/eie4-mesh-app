package com.example.disastermesh.core.database

enum class MessageType(val id: Int) { BROADCAST(1), NODE(2), USER(3);
    companion object { fun fromId(i:Int)=values().first{it.id==i} }}