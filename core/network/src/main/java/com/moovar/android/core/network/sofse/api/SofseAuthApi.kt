package com.moovar.android.core.network.sofse.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SofseAuthApi {
    @FormUrlEncoded
    @POST("auth/token")
    fun getToken(@Field("hash") hash: String): Call<TokenResponse>
}

@Serializable
data class TokenResponse(
    @SerialName("token") val token: String
)
