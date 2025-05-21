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
    LIST_USERS_RESP   (0x08),
    GATEWAY_AVAILABLE (0x09),      // node → app  (enables UI switch)
    USER_MSG_GATEWAY  (0x0A),      // app  → node (send via gateway)
    ACK_SUCCESS       (0x0C),
    BLE_PUBKEY_RESP       (0x0F),     // node → app – 32 B public-key of user
    BLE_REQUEST_PUBKEY     (0x0E);     // app → node – “give me user X”
    //    ANNOUNCE_KEY      (0x0D),     // app → node – my 32 B public key
//    ENC_USER_MSG      (0x11);     // user↔user ciphertext
    companion object { fun fromId(i: Int) = entries.firstOrNull { it.id == i } }
}

/** Small helper for list replies */
data class ListResponse(val opcode: Opcode, val ids: List<Int>)

data object GatewayAvailable
data class GatewayChatMessage(     // identical to ChatMessage but flagged
    val inner: ChatMessage
)

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
        val buf  = ByteBuffer.allocate(1 + 4 + 4 + 4 + body.size)
            .order(ByteOrder.LITTLE_ENDIAN)

        buf.put(msg.type.id.toByte())
        buf.putInt(msg.pktId.toInt())
        buf.putInt(msg.dest)
        buf.putInt(msg.sender)
        buf.put(body)
        return buf.array()
    }

    fun encodeGateway(cm: ChatMessage): ByteArray =
        encode(cm).also { it[0] = Opcode.USER_MSG_GATEWAY.id.toByte() }


    fun encodeRequestPubKey(targetUid: Int): ByteArray =
        ByteBuffer.allocate(1 + 4 + 4)             // header only
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put(Opcode.BLE_REQUEST_PUBKEY.id.toByte())
                putInt(targetUid)      // dest = user we need the key for
                putInt(0)              // sender = 0   (ignored by node)
            }.array()

    /* --------------------------- decode -------------------------------- */

    fun decode(bytes: ByteArray): Any? = runCatching {
        if (bytes.isEmpty()) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val op = Opcode.fromId(buf.get().toInt()) ?: return null

        when (op) {

            Opcode.LIST_NODES_RESP, Opcode.LIST_USERS_RESP -> {
                /* skip dest / sender (2 × 4 B) */
                if (buf.remaining() < 12) return null
                buf.int; buf.int
                if (buf.remaining() < 4) return null
                val n = buf.int
                if (buf.remaining() < n * 4) return null
                val list = List(n) { buf.int }
                ListResponse(op, list)
            }

            Opcode.ACK_SUCCESS -> {
                if (buf.remaining() < 4) return null
                println("Received ACK")
                val pid = buf.int.toUInt()
                AckSuccess(pid)
            }

            Opcode.BROADCAST, Opcode.NODE_MSG, Opcode.USER_MSG -> {
                if (buf.remaining() < 12) return null      // pktId + dest + sender
                val pid    = buf.int.toUInt()
                val dest   = buf.int
                val sender = buf.int
                val payload = ByteArray(buf.remaining())
                buf.get(payload)
                ChatMessage(
                    pktId  = pid,
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

            Opcode.USER_MSG_GATEWAY -> {
                if (buf.remaining() < 12) return null
                val pid    = buf.int.toUInt()
                val dest   = buf.int
                val sender = buf.int
                val payload = ByteArray(buf.remaining())
                buf.get(payload)
                GatewayChatMessage(                         // wrapper tells repo to switch
                    ChatMessage(
                        pktId  = pid,
                        type   = MessageType.USER,
                        dest   = dest,
                        sender = sender,
                        body   = String(payload, Charsets.UTF_8)
                    )
                )
            }


            Opcode.GATEWAY_AVAILABLE -> {
                GatewayAvailable
            }


            Opcode.BLE_PUBKEY_RESP -> {             //  ← NEW
                if (buf.remaining() < 40) return null   // dest+sender+32 B key
                val uid = buf.int                      // dest = owner of key
                buf.int                                // sender (ignored)
                val key = ByteArray(32)
                buf.get(key)
                PubKeyResp(uid, key)
            }

            else -> null
        }
    }.getOrNull()
}


