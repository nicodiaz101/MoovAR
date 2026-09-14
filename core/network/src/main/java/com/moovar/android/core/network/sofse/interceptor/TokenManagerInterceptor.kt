package com.moovar.android.core.network.sofse.interceptor

import com.moovar.android.core.network.sofse.TokenStorage
import com.moovar.android.core.network.sofse.api.SofseAuthApi
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

class TokenManagerInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApi: SofseAuthApi
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenStorage.getToken()
        val authenticatedRequest = originalRequest.withToken(token)
        val response = chain.proceed(authenticatedRequest)

        return if (response.code == 401) {
            response.close()
            
            val newToken = synchronized(this) {
                // Check if another thread already refreshed the token
                val currentToken = tokenStorage.getToken()
                if (currentToken != null && currentToken != token) {
                    currentToken
                } else {
                    val refreshed = refreshToken()
                    tokenStorage.saveToken(refreshed)
                    refreshed
                }
            }
            
            chain.proceed(originalRequest.withToken(newToken))
        } else {
            response
        }
    }

    private fun refreshToken(): String {
        val versionString = "V3rS10n\$SOFSE"
        val md5Hash = MessageDigest.getInstance("MD5")
            .digest(versionString.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val tokenResponse = authApi.getToken(md5Hash).execute()
        return tokenResponse.body()?.token
            ?: throw IOException("No se pudo obtener el token de SOFSE")
    }

    private fun Request.withToken(token: String?): Request =
        if (token != null) newBuilder().header("Authorization", "Bearer $token").build()
        else this
}
