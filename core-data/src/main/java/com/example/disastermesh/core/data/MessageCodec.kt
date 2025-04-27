package com.example.disastermesh.core.data

import com.example.disastermesh.core.database.MessageType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
Message types:
Broadcast: 1
Node: 2
User: 3

UserID update: TBC => 4
 */

/**
 *  [TYPE 1 B] [DEST-A 4 B] [DEST-B 4 B] [UTF-8 PAYLOAD …]
 *  Little-endian network byte order.
 */

object MessageCodec {

    /* --------------------------- encode -------------------------------- */

    fun encode(msg: ChatMessage): ByteArray {
        val body = msg.body.toByteArray(Charsets.UTF_8)
        val buf  = ByteBuffer.allocate(1 + 4 + 4 + body.size)
            .order(ByteOrder.LITTLE_ENDIAN)

        buf.put(msg.type.id.toByte())
        buf.putInt(msg.destA)
        buf.putInt(msg.destB)
        buf.put(body)
        return buf.array()
    }

    /* --------------------------- decode -------------------------------- */

    fun decode(bytes: ByteArray): ChatMessage? {
        return try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            if (buf.remaining() < 9) return null           // header incomplete

            val type  = MessageType.fromId(buf.get().toInt())
            val destA = buf.int
            val destB = buf.int

            val payload = ByteArray(buf.remaining())
            buf.get(payload)

            ChatMessage(
                type  = type,
                destA = destA,
                destB = destB,
                body  = String(payload, Charsets.UTF_8)
            )
        } catch (e: Exception) {
            null
        }
    }
}
