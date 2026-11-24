package com.vidrierapp.data.remote.api

import com.vidrierapp.data.remote.dto.ReportRentedResponseDto
import com.vidrierapp.data.remote.dto.ShopDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockInterceptorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val shopsJson = """
        [
          {"id":"a","address":"A","neighborhood":"N","latitude":0.0,"longitude":0.0,"surfaceAreaM2":1,"rentalPrice":"1","topCategoryName":"Cafe"},
          {"id":"b","address":"B","neighborhood":"N","latitude":0.0,"longitude":0.0,"surfaceAreaM2":1,"rentalPrice":"1","topCategoryName":"Cafe"},
          {"id":"r","address":"R","neighborhood":"N","latitude":0.0,"longitude":0.0,"surfaceAreaM2":1,"rentalPrice":"1","topCategoryName":"Cafe","listingStatus":"RENTED"}
        ]
    """.trimIndent()

    private val client = OkHttpClient.Builder()
        .addInterceptor(MockInterceptor(json, { shopsJson }))
        .build()

    private fun get(path: String) = client.newCall(
        Request.Builder().url("https://api.example.com/$path").build()
    ).execute()

    private fun post(path: String) = client.newCall(
        Request.Builder().url("https://api.example.com/$path")
            .post("".toRequestBody()).build()
    ).execute()

    private fun report(id: String): ReportRentedResponseDto =
        post("api/v1/shops/$id/report-rented").use {
            json.decodeFromString(ReportRentedResponseDto.serializer(), it.body.string())
        }

    private fun list(): List<ShopDto> = get("api/v1/shops").use {
        json.decodeFromString(ListSerializer(ShopDto.serializer()), it.body.string())
    }

    private fun detail(id: String): ShopDto = get("api/v1/shops/$id").use {
        json.decodeFromString(ShopDto.serializer(), it.body.string())
    }

    @Test
    fun `threshold is three reports`() {
        assertEquals(3, MOCK_REPORT_THRESHOLD)
    }

    @Test
    fun `reports increment the counter and keep the shop active below the threshold`() {
        assertEquals(1, report("a").reportCount)
        val second = report("a")

        assertEquals(2, second.reportCount)
        assertEquals("ACTIVE", second.listingStatus)
        assertEquals("ACTIVE", detail("a").listingStatus)
    }

    @Test
    fun `third report moves the shop to UNDER_REVIEW and it still appears in the list`() {
        report("a")
        report("a")
        val third = report("a")

        assertEquals("UNDER_REVIEW", third.listingStatus)
        assertEquals("UNDER_REVIEW", detail("a").listingStatus)
        val listed = list().associateBy { it.id }
        assertEquals("UNDER_REVIEW", listed.getValue("a").listingStatus)
        assertEquals("ACTIVE", listed.getValue("b").listingStatus)
    }

    @Test
    fun `counters are per shop`() {
        report("a")
        report("a")

        assertEquals(1, report("b").reportCount)
    }

    @Test
    fun `rented shops are excluded from the list but available by id`() {
        assertEquals(listOf("a", "b"), list().map { it.id })
        assertEquals("RENTED", detail("r").listingStatus)
    }

    @Test
    fun `rented shops are excluded from category filtered lists`() {
        val filtered = get("api/v1/shops?category=Cafe").use {
            json.decodeFromString(ListSerializer(ShopDto.serializer()), it.body.string())
        }

        assertFalse(filtered.any { it.id == "r" })
        assertTrue(filtered.isNotEmpty())
    }

    @Test
    fun `reporting an unknown shop returns 404`() {
        assertEquals(404, post("api/v1/shops/zzz/report-rented").use { it.code })
    }
}
