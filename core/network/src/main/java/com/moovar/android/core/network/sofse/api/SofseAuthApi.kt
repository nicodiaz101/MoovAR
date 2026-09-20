package com.moovar.android.core.network.sofse.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface SofseAuthApi {
    @Headers("Content-Type: application/json")
    @POST("auth/authorize")
    fun authorize(@Body request: SofseAuthRequest): Call<TokenResponse>
}

@Serializable
data class SofseAuthRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String
)

@Serializable
data class TokenResponse(
    @SerialName("token") val token: String
)
