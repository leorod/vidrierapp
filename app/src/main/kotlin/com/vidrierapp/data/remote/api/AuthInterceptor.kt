package com.vidrierapp.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val token: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            token()?.let { header("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}
