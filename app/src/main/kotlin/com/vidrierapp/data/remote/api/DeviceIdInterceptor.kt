package com.vidrierapp.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

class DeviceIdInterceptor(private val deviceId: () -> String) : Interceptor {

    private val cachedId: String by lazy(deviceId)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Device-Id", cachedId)
            .build()
        return chain.proceed(request)
    }
}
