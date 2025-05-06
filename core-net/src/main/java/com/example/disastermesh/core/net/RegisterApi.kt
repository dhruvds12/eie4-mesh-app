
package com.example.disastermesh.core.net

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RegisterApi {

    @POST("/v1/register/user")
    suspend fun register(@Body body: RegisterReq): Response<Unit>
}

data class RegisterReq(
    @SerializedName("userID")      val userId: String,
    @SerializedName("phoneDigits") val phone:  String,
    @SerializedName("displayName") val name:   String
)
