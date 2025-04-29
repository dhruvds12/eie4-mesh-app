package com.example.disastermesh.core

/**  Maximum UTF-8 characters we ever try to write over BLE  */
const val MAX_MSG_CHARS = 180

fun phoneToUserId(e164: String): Int {
    // e.g. "+44 7911 123456"  →  "7911123456"
    val digits = e164.dropWhile { !it.isDigit() }      // strip '+'
        .dropWhile { it == '0' }           // drop leading country code 0s
        .take(10)                          // keep at most 10 digits
    return digits.fold(0) { acc, c -> (acc shl 3) xor (acc shr 29) xor (c - '0') }
        .ushr(0)                              // unsigned-to-Int (32 bit)
}


