package com.moovar.android.core.network.sofse.interceptor

import android.util.Log
import com.moovar.android.core.network.sofse.SofseCredentialsGenerator
import com.moovar.android.core.network.sofse.TokenStorage
import com.moovar.android.core.network.sofse.api.SofseAuthApi
import com.moovar.android.core.network.sofse.api.SofseAuthRequest
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class TokenManagerInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApi: SofseAuthApi
) : Interceptor {

    companion object {
        private const val TAG = "TokenManagerInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Ensure token is present before first request
        var token = tokenStorage.getToken()
        if (token.isNullOrBlank()) {
            token = synchronized(this) {
                tokenStorage.getToken() ?: try {
                    val newToken = refreshToken()
                    tokenStorage.saveToken(newToken)
                    newToken
                } catch (e: Exception) {
                    Log.w(TAG, "Initial token fetch failed: ${e.message}")
                    null
                }
            }
        }

        val authenticatedRequest = originalRequest.withToken(token)
        val response = chain.proceed(authenticatedRequest)

        // 2. If unauthorized or forbidden, refresh token and retry once
        return if (response.code == 401 || response.code == 403) {
            val newToken = synchronized(this) {
                val currentToken = tokenStorage.getToken()
                if (!currentToken.isNullOrBlank() && currentToken != token) {
                    currentToken
                } else {
                    try {
                        val refreshed = refreshToken()
                        tokenStorage.saveToken(refreshed)
                        refreshed
                    } catch (e: Exception) {
                        Log.e(TAG, "Token refresh failed: ${e.message}")
                        null
                    }
                }
            }

            if (!newToken.isNullOrBlank()) {
                response.close()
                chain.proceed(originalRequest.withToken(newToken))
            } else {
                response
            }
        } else {
            response
        }
    }

    private fun refreshToken(): String {
        val (username, password) = SofseCredentialsGenerator.generateCredentials()
        val call = authApi.authorize(SofseAuthRequest(username = username, password = password))
        val response = call.execute()
        if (!response.isSuccessful || response.body()?.token == null) {
            throw IOException("Fallo autorización SOFSE: code ${response.code()}")
        }
        return response.body()!!.token
    }

    /**
     * SOFSE v1 API expects the JWT token directly in the Authorization header (without 'Bearer ' prefix).
     */
    private fun Request.withToken(token: String?): Request =
        if (!token.isNullOrBlank()) newBuilder().header("Authorization", token).build()
        else this
}
