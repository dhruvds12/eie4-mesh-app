package com.example.disastermesh.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.disastermesh.core.database.dao.ChatDao
import com.example.disastermesh.core.database.dao.PublicKeyDao
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.entities.PublicKey

@Database(
    entities = [Chat::class, Message::class, PublicKey::class],
    version  = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MeshDb : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun publicKeyDao() : PublicKeyDao


}