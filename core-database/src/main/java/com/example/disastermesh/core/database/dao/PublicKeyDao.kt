package com.example.disastermesh.core.database.dao

import androidx.room.*
import com.example.disastermesh.core.database.entities.PublicKey
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pk: PublicKey)

    @Query("SELECT pubKey FROM PublicKey WHERE userId = :uid")
    fun keyFlow(uid: Int): Flow<ByteArray?>

    @Query("SELECT pubKey FROM PublicKey WHERE userId = :uid")
    suspend fun key(uid: Int): ByteArray?
}
