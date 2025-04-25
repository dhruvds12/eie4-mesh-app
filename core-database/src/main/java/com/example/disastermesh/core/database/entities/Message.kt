package com.example.disastermesh.core.database.entities


import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One message inside a chat.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity      = Chat::class,
            parentColumns = ["id"],
            childColumns  = ["chatId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val msgId: Long = 0,
    val chatId : Long,
    val mine   : Boolean,                  // true = I sent it
    val ts     : Long = System.currentTimeMillis(),
    val body   : String
)
