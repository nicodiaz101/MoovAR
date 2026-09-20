package com.moovar.android.core.network.sofse.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class StaticHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("Accept", "application/json")
                .build()
        )
}
