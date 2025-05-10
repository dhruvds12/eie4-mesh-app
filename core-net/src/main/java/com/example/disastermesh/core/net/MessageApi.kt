package com.example.disastermesh.core.net

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/* ---------------- DTOs ---------------- */

data class NetMessage(
    @SerializedName("msgID") val id:   String = "",
    @SerializedName("src")   val src:  String,
    @SerializedName("dst")   val dst:  String,
    @SerializedName("body")  val body: String,
    @SerializedName("ts")    val ts:   Long   = 0
)

data class UserSyncReq(
    @SerializedName("userID") val userId: String,
    @SerializedName("since")  val since:  Long = 0,
    @SerializedName("uplink") val uplink: List<NetMessage> = emptyList()
)

data class SyncResp(
    @SerializedName("ack")   val ack:   List<String>,
    @SerializedName("down")  val down:  List<NetMessage>,
    @SerializedName("sleep") val sleep: Long
)

/* ---------------- API ---------------- */

interface MessageApi {

    @POST("/v1/message")
    suspend fun post(@Body m: NetMessage): Response<Unit>

    @POST("/v1/sync/user")
    suspend fun sync(@Body req: UserSyncReq): Response<SyncResp>
}
