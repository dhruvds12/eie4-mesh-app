package com.example.disastermesh.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stores the 32-byte Curve25519 public key for one remote user. */
@Entity
data class PublicKey(
    @PrimaryKey val userId: Int,
    val pubKey: ByteArray           // 32 bytes
)
