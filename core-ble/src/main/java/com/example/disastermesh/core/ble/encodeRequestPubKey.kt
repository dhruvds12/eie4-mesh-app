package com.example.disastermesh.core.ble

import com.example.disastermesh.core.data.MessageCodec

fun encodeRequestPubKey(uid: Int): ByteArray =
    MessageCodec.encodeRequestPubKey(uid)
