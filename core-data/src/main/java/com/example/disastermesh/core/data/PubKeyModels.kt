package com.example.disastermesh.core.data

/** remote → app : contains <dest=userId, key[32]> */
data class PubKeyResp(val userId: Int, val key: ByteArray)