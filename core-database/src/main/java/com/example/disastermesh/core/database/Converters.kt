package com.example.disastermesh.core.database

import androidx.room.TypeConverter
import com.example.disastermesh.core.database.entities.Route

class Converters {
    @TypeConverter
    fun typeToInt(t: MessageType): Int = t.id

    @TypeConverter
    fun intToType(i: Int): MessageType = MessageType.fromId(i)

    @TypeConverter
    fun routeToString(r: Route): String = r.name

    @TypeConverter
    fun stringToRoute(s: String): Route = Route.valueOf(s)
}