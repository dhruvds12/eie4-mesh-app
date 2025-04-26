package com.example.disastermesh.core.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun typeToInt(t: MessageType): Int = t.id

    @TypeConverter
    fun intToType(i: Int): MessageType = MessageType.fromId(i)
}