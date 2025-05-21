package com.example.disastermesh.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.disastermesh.core.database.dao.ChatDao
import com.example.disastermesh.core.database.dao.PublicKeyDao
import com.example.disastermesh.core.database.entities.Chat
import com.example.disastermesh.core.database.entities.Message
import com.example.disastermesh.core.database.entities.PublicKey

@Database(
    entities = [Chat::class, Message::class, PublicKey::class],
    version  = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MeshDb : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun publicKeyDao() : PublicKeyDao

    companion object {
        /** adds ‘encrypted’ column & creates PublicKey table */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
            ALTER TABLE Chat ADD COLUMN encrypted
            INTEGER NOT NULL DEFAULT 0
        """.trimIndent())

                /* column name MUST be “pubKey” to match the entity */
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS PublicKey(
                userId INTEGER NOT NULL PRIMARY KEY,
                pubKey BLOB    NOT NULL
            )
        """.trimIndent())
            }
        }

    }


}