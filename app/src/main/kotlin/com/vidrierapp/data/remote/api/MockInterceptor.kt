package com.vidrierapp.data.remote.api

import android.content.Context
import com.vidrierapp.data.remote.dto.ReportRentedResponseDto
import com.vidrierapp.data.remote.dto.ShopDto
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

const val MOCK_REPORT_THRESHOLD = 3

class MockInterceptor(
    private val json: Json,
    private val readShopsJson: () -> String,
    private val reportThreshold: Int = MOCK_REPORT_THRESHOLD
) : Interceptor {

    constructor(context: Context, json: Json) : this(
        json = json,
        readShopsJson = {
            try {
                context.assets.open("mock_shops.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "[]"
            }
        }
    )

    private val reportCounts = ConcurrentHashMap<String, Int>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val pathSegments = url.encodedPathSegments

        val shops = try {
            json.decodeFromString<List<ShopDto>>(readShopsJson())
        } catch (e: Exception) {
            emptyList()
        }.map { withReportedStatus(it) }

        val isReport = request.method == "POST" &&
            pathSegments.size >= 5 &&
            pathSegments.last() == "report-rented" &&
            pathSegments[pathSegments.size - 3] == "shops"

        if (isReport) {
            val shopId = pathSegments[pathSegments.size - 2]
            val known = shops.any { it.id == shopId }
            if (!known) return respond(request, 404, "Not Found", "{}")
            val count = reportCounts.merge(shopId, 1, Int::plus) ?: 1
            val status = if (count >= reportThreshold) STATUS_UNDER_REVIEW else shops.first { it.id == shopId }.listingStatus
            val body = json.encodeToString(
                ReportRentedResponseDto.serializer(),
                ReportRentedResponseDto(shopId = shopId, reportCount = count, listingStatus = status)
            )
            return respond(request, 200, "OK", body)
        }

        val responseBodyString = when {
            pathSegments.size >= 4 && pathSegments[pathSegments.size - 2] == "shops" -> {
                val shopId = pathSegments.last()
                val shop = shops.find { it.id == shopId } ?: shops.firstOrNull()
                if (shop != null) {
                    json.encodeToString(ShopDto.serializer(), shop)
                } else {
                    "{}"
                }
            }
            else -> {
                val categoryQuery = url.queryParameter("category")
                val available = shops.filter { it.listingStatus != STATUS_RENTED }
                val filteredShops = if (!categoryQuery.isNullOrBlank() && categoryQuery != "Todos") {
                    available.filter { it.topCategoryName.equals(categoryQuery, ignoreCase = true) }
                } else {
                    available
                }
                json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ShopDto.serializer()), filteredShops)
            }
        }

        return respond(request, 200, "OK", responseBodyString)
    }

    private fun withReportedStatus(shop: ShopDto): ShopDto {
        val count = reportCounts[shop.id] ?: 0
        return if (count >= reportThreshold && shop.listingStatus == "ACTIVE") {
            shop.copy(listingStatus = STATUS_UNDER_REVIEW)
        } else {
            shop
        }
    }

    private fun respond(
        request: okhttp3.Request,
        code: Int,
        message: String,
        body: String
    ): Response = Response.Builder()
        .code(code)
        .message(message)
        .protocol(Protocol.HTTP_1_1)
        .request(request)
        .body(body.toResponseBody("application/json".toMediaType()))
        .addHeader("Content-Type", "application/json")
        .build()

    private companion object {
        const val STATUS_UNDER_REVIEW = "UNDER_REVIEW"
        const val STATUS_RENTED = "RENTED"
    }
}
