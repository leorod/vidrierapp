package com.vidrierapp.data.remote.api

import android.content.Context
import com.vidrierapp.data.remote.dto.ShopDto
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor(
    private val context: Context,
    private val json: Json
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val pathSegments = url.encodedPathSegments

        val jsonString = try {
            context.assets.open("mock_shops.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "[]"
        }

        val shops = try {
            json.decodeFromString<List<ShopDto>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }

        val responseBodyString = when {
            pathSegments.size >= 4 && pathSegments[pathSegments.size - 2] == "shops" -> {
                // /api/v1/shops/{id}
                val shopId = pathSegments.last()
                val shop = shops.find { it.id == shopId } ?: shops.firstOrNull()
                if (shop != null) {
                    json.encodeToString(ShopDto.serializer(), shop)
                } else {
                    "{}"
                }
            }
            else -> {
                // /api/v1/shops with optional query category
                val categoryQuery = url.queryParameter("category")
                val filteredShops = if (!categoryQuery.isNullOrBlank() && categoryQuery != "Todos") {
                    shops.filter { it.topCategoryName.equals(categoryQuery, ignoreCase = true) }
                } else {
                    shops
                }
                json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ShopDto.serializer()), filteredShops)
            }
        }

        return Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .body(responseBodyString.toResponseBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
    }
}
