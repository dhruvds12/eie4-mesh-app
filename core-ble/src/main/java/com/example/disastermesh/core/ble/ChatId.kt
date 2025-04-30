package com.example.disastermesh.core.ble

import com.example.disastermesh.core.database.MessageType

/** Builds a unique chat ID:
 *  BROADCAST → 0
 *  NODE/USER → (type.id  << 32) | unsigned(target)
 */
fun makeChatId(type: MessageType, target: Int): Long =
    if (type == MessageType.BROADCAST) 0L
    else  (type.id.toLong() shl 32) or (target.toLong() and 0xFFFF_FFFFL)

fun idType(cid: Long): MessageType =
    if (cid == 0L) MessageType.BROADCAST
    else MessageType.fromId((cid ushr 32).toInt())

fun idTarget(cid: Long): Int = (cid and 0xFFFF_FFFFL).toInt()
