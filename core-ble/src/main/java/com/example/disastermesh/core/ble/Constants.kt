package com.example.disastermesh.core.ble

/**  Maximum UTF-8 characters we ever try to write over BLE  */
const val MAX_MSG_CHARS = 180

//fun phoneToUserId(e164: String): Int {
//    // e.g. "+44 7911 123456"  →  "7911123456"
//    val digits = e164.dropWhile { !it.isDigit() }      // strip '+'
//        .dropWhile { it == '0' }           // drop leading country code 0s
//        .take(10)                          // keep at most 10 digits
//    return digits.fold(0) { acc, c -> (acc shl 3) xor (acc shr 29) xor (c - '0') }
//        .ushr(0)                              // unsigned-to-Int (32 bit)
//}

fun phoneToUserId(e164: String): Int =
    e164.filter { it.isDigit() }          // keep digits only
        .takeLast(9)                      // at most 9 → fits 32-bit
        .toLong()
        .toInt()

fun encodeUserIdUpdate(uid: Int): ByteArray =
    java.nio.ByteBuffer.allocate(1 + 4 + 4)      // 1 B type + 8 B header
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .apply {
            put(0x04)          // BLEMessageType.USER_ID_UPDATE
            putInt(0)          // destA = 0
            putInt(uid)        // sender = my user-id
        }
        .array()