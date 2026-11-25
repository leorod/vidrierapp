package com.vidrierapp.data.remote.api

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdInterceptorTest {

    @Test
    fun `adds the device id header and resolves the id once`() {
        var lookups = 0
        val seenHeaders = mutableListOf<String?>()
        val client = OkHttpClient.Builder()
            .addInterceptor(DeviceIdInterceptor { lookups++; "device-1" })
            .addInterceptor { chain ->
                seenHeaders += chain.request().header("X-Device-Id")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .build()
            }
            .build()
        val request = Request.Builder().url("http://localhost/api").build()

        client.newCall(request).execute().close()
        client.newCall(request).execute().close()

        assertEquals(listOf<String?>("device-1", "device-1"), seenHeaders)
        assertEquals(1, lookups)
    }
}
