package com.example.disastermesh.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 7 → 8 : rewrite Chat.title using unsigned notation for NODE + USER chats */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // Iterate once – tiny table, so a cursor loop is fine
        val cur = db.query("SELECT id, type FROM Chat")
        while (cur.moveToNext()) {
            val id   = cur.getLong(0)
            val type = cur.getInt(1)        // 2 = NODE, 3 = USER

            if (type == 2 || type == 3) {
                val unsigned = (id and 0xFFFF_FFFF).toUInt()
                val newTitle = if (type == 2) "Node $unsigned" else "User $unsigned"
                db.execSQL("UPDATE Chat SET title = ? WHERE id = ?", arrayOf(newTitle, id))
            }
        }
        cur.close()
    }
}