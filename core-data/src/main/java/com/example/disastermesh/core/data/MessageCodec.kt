package com.example.disastermesh.core.data

import com.example.disastermesh.core.database.MessageType
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class Opcode(val id: Int) {
    BROADCAST         (0x01),
    NODE_MSG          (0x02),
    USER_MSG          (0x03),
    USER_ID_UPDATE    (0x04),
    LIST_NODES_REQ    (0x05),
    LIST_USERS_REQ    (0x06),
    LIST_NODES_RESP   (0x07),
    LIST_USERS_RESP   (0x08);

    companion object { fun fromId(i: Int) = entries.firstOrNull { it.id == i } }
}

/** Small helper for list replies */
data class ListResponse(val opcode: Opcode, val ids: List<Int>)


/*
Message types:
Broadcast: 1
Node: 2
User: 3
 */

/**
 *  [TYPE 1 B] [DEST-A 4 B] [DEST-B 4 B] [UTF-8 PAYLOAD …]
 *  Little-endian network byte order.
 */

object MessageCodec {
    fun encodeListRequest(op: Opcode): ByteArray {
        /* header only – dest / sender = 0 */
        return byteArrayOf(op.id.toByte()) + ByteArray(8)
    }


    /* --------------------------- encode -------------------------------- */

    fun encode(msg: ChatMessage): ByteArray {
        val body = msg.body.toByteArray(Charsets.UTF_8)
        val buf  = ByteBuffer.allocate(1 + 4 + 4 + body.size)
            .order(ByteOrder.LITTLE_ENDIAN)

        buf.put(msg.type.id.toByte())
        buf.putInt(msg.dest)
        buf.putInt(msg.sender)
        buf.put(body)
        return buf.array()
    }

    /* --------------------------- decode -------------------------------- */

    fun decode(bytes: ByteArray): Any? = runCatching {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buf.remaining() < 9) return null            // header incomplete

        when (val op = Opcode.fromId(buf.get().toInt())) {

            Opcode.LIST_NODES_RESP, Opcode.LIST_USERS_RESP -> {
                /* skip dest / sender (2 × 4 B) */
                buf.int; buf.int
                if (buf.remaining() < 4) return null
                val n = buf.int
                if (buf.remaining() < n * 4) return null
                val list = List(n) { buf.int }
                ListResponse(op, list)
            }

            Opcode.BROADCAST, Opcode.NODE_MSG, Opcode.USER_MSG -> {
                val dest   = buf.int
                val sender = buf.int
                val payload = ByteArray(buf.remaining())
                buf.get(payload)
                ChatMessage(
                    type   = when (op) {
                        Opcode.BROADCAST -> MessageType.BROADCAST
                        Opcode.NODE_MSG  -> MessageType.NODE
                        else             -> MessageType.USER
                    },
                    dest   = dest,
                    sender = sender,
                    body   = String(payload, Charsets.UTF_8)
                )
            }

            else -> null
        }
    }.getOrNull()
}
