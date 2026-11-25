package com.vidrierapp.server.routes

import com.vidrierapp.server.configureApp
import com.vidrierapp.server.model.ReportRentedResponse
import com.vidrierapp.server.model.ShopDetailResponse
import com.vidrierapp.server.model.ShopSummaryResponse
import com.vidrierapp.server.model.TrialResponse
import com.vidrierapp.server.service.CategoryNormalizer
import com.vidrierapp.server.service.DuplicateVoteException
import com.vidrierapp.server.service.GoogleGeocodingService
import com.vidrierapp.server.service.MockPaymentProcessor
import com.vidrierapp.server.service.NormalizedCategory
import com.vidrierapp.server.service.RateLimitExceededException
import com.vidrierapp.server.service.VidrierAppRepository
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val SHOP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111")
private const val FINGERPRINT = "0123456789abcdef0123456789abcdef"
private const val TOKEN = "browser-token-0123456789"
private const val ADMIN_TOKEN = "secret-admin"

private val unusedDataSource: DataSource = Proxy.newProxyInstance(
    DataSource::class.java.classLoader,
    arrayOf(DataSource::class.java)
) { _, _, _ -> error("Database must not be used in route tests") } as DataSource

private class FakeRepository : VidrierAppRepository(unusedDataSource) {
    var duplicate = false
    var rateLimited = false
    var lastCategory: NormalizedCategory? = null
    var lastBrowserToken: String? = null
    var statusUpdates = mutableListOf<Pair<UUID, String>>()
    var activeDevices = setOf<String>()
    var sampleSufficient = true

    override fun categoryAliases() = mapOf("cafeteria" to "cafeteria", "verduleria" to "verduleria")

    override fun shops(category: String?, bounds: com.vidrierapp.server.service.Bounds?) = listOf(
        ShopSummaryResponse(SHOP_ID.toString(), "Alberdi 6420", "Mataderos", -34.65, -58.5, 70, "ARS 900.000", "Cafetería", 18, "AVAILABLE")
    )

    override fun shopDetail(id: UUID, metricsAllowed: Boolean): ShopDetailResponse {
        if (id != SHOP_ID) throw NoSuchElementException("Shop not found")
        val base = ShopDetailResponse(id.toString(), "Alberdi 6420", "Mataderos", -34.65, -58.5, totalVotes = 18, status = "AVAILABLE", firstVoteAt = Instant.parse("2026-11-01T10:00:00Z").toEpochMilli(), lastVoteAt = Instant.parse("2026-11-20T10:00:00Z").toEpochMilli(), isSampleSufficient = sampleSufficient, metricsAvailable = false)
        return if (metricsAllowed && sampleSufficient) base.copy(metricsAvailable = true, frequencyCounts = mapOf("DIARIA" to 4)) else base
    }

    override fun hasActiveAccess(deviceId: String?) = deviceId != null && deviceId in activeDevices

    override fun submitVote(shopId: UUID, category: NormalizedCategory, frequency: String?, fingerprint: String, browserToken: String, ipAddress: String): UUID {
        if (duplicate) throw DuplicateVoteException()
        if (rateLimited) throw RateLimitExceededException()
        lastCategory = category
        lastBrowserToken = browserToken
        return UUID.randomUUID()
    }

    override fun reportRented(id: UUID): ReportRentedResponse {
        if (id != SHOP_ID) throw NoSuchElementException("Shop not found")
        return ReportRentedResponse(id.toString(), 1, "ACTIVE")
    }

    override fun startTrial(deviceId: String) = TrialResponse(deviceId, "TRIAL_ACTIVE", Instant.parse("2026-11-28T00:00:00Z").toString(), true)

    override fun updateShopStatus(id: UUID, status: String) { statusUpdates += id to status }

    override fun expireStaleShops() = 2

    override fun expireAccessStatuses() = 1

    override fun refreshAggregates() = true
}

private fun apiTest(repository: FakeRepository = FakeRepository(), block: suspend ApplicationTestBuilder.(FakeRepository) -> Unit) = testApplication {
    application { configureApp(repository, MockPaymentProcessor(), GoogleGeocodingService(""), ADMIN_TOKEN, null) }
    block(repository)
}

private suspend fun ApplicationTestBuilder.postJson(path: String, body: String, headers: Map<String, String> = emptyMap()) =
    client.post(path) {
        contentType(ContentType.Application.Json)
        headers.forEach { (name, value) -> header(name, value) }
        setBody(body)
    }

class ApiRoutesTest {

    @Test
    fun `vote accepts web payload with identifiers in the body`() = apiTest { repository ->
        val response = postJson("/api/v1/votes", """{"localId":"$SHOP_ID","category":"Cafetería","frequency":"DAILY","clientToken":"$TOKEN","fingerprintHash":"$FINGERPRINT"}""")
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("cafeteria", repository.lastCategory?.slug)
        assertEquals(TOKEN, repository.lastBrowserToken)
    }

    @Test
    fun `vote accepts identifiers sent as headers`() = apiTest {
        val response = postJson(
            "/api/v1/votes",
            """{"shopId":"$SHOP_ID","category":"verduleria"}""",
            mapOf("X-Fingerprint-Hash" to FINGERPRINT, "X-Browser-Token" to TOKEN)
        )
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `vote without fingerprint is rejected`() = apiTest {
        val response = postJson("/api/v1/votes", """{"shopId":"$SHOP_ID","category":"cafeteria","clientToken":"$TOKEN"}""")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `free text vote is normalized with fuzzy matching`() = apiTest { repository ->
        val response = postJson("/api/v1/votes", """{"localId":"$SHOP_ID","otherText":"verduleris","clientToken":"$TOKEN","fingerprintHash":"$FINGERPRINT"}""")
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("verduleria", repository.lastCategory?.slug)
    }

    @Test
    fun `duplicate vote answers conflict`() = apiTest(FakeRepository().apply { duplicate = true }) {
        val response = postJson("/api/v1/votes", """{"localId":"$SHOP_ID","category":"cafeteria","clientToken":"$TOKEN","fingerprintHash":"$FINGERPRINT"}""")
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `rate limited vote answers too many requests`() = apiTest(FakeRepository().apply { rateLimited = true }) {
        val response = postJson("/api/v1/votes", """{"localId":"$SHOP_ID","category":"cafeteria","clientToken":"$TOKEN","fingerprintHash":"$FINGERPRINT"}""")
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
    }

    @Test
    fun `shop list returns available shops`() = apiTest {
        val response = client.get("/api/v1/shops")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Mataderos"))
    }

    @Test
    fun `detail hides metrics without an active pass`() = apiTest {
        val body = client.get("/api/v1/shops/$SHOP_ID").bodyAsText()
        assertTrue(body.contains("\"metricsAvailable\":false"))
    }

    @Test
    fun `detail exposes metrics with an active pass and enough votes`() = apiTest(FakeRepository().apply { activeDevices = setOf("device-1") }) {
        val body = client.get("/api/v1/shops/$SHOP_ID") { header("X-Device-Id", "device-1") }.bodyAsText()
        assertTrue(body.contains("\"metricsAvailable\":true"))
        assertTrue(body.contains("\"DIARIA\":4"))
    }

    @Test
    fun `detail hides metrics below the sample threshold even with a pass`() = apiTest(FakeRepository().apply { activeDevices = setOf("device-1"); sampleSufficient = false }) {
        val body = client.get("/api/v1/shops/$SHOP_ID") { header("X-Device-Id", "device-1") }.bodyAsText()
        assertTrue(body.contains("\"metricsAvailable\":false"))
    }

    @Test
    fun `unknown shop answers not found`() = apiTest {
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/shops/${UUID.randomUUID()}").status)
    }

    @Test
    fun `malformed shop id answers bad request`() = apiTest {
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/shops/not-a-uuid").status)
    }

    @Test
    fun `report rented answers the shape the app expects`() = apiTest {
        val response = postJson("/api/v1/shops/$SHOP_ID/report-rented", "{}")
        assertEquals(HttpStatusCode.Accepted, response.status)
        assertTrue(response.bodyAsText().contains("\"reportCount\":1"))
    }

    @Test
    fun `trial endpoint starts a trial for the device`() = apiTest {
        val response = postJson("/api/v1/auth/trial", """{"deviceId":"device-1"}""")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"metricsAvailable\":true"))
    }

    @Test
    fun `trial endpoint requires a device id`() = apiTest {
        assertEquals(HttpStatusCode.BadRequest, postJson("/api/v1/auth/trial", """{"deviceId":""}""").status)
    }

    @Test
    fun `admin endpoints reject missing token`() = apiTest {
        val response = postJson("/api/v1/admin/shops/$SHOP_ID/status", """{"status":"RENTED"}""")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `admin can change shop status with the token`() = apiTest { repository ->
        val response = postJson("/api/v1/admin/shops/$SHOP_ID/status", """{"status":"RENTED"}""", mapOf(HttpHeaders.Authorization to "Bearer $ADMIN_TOKEN"))
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(listOf(SHOP_ID to "RENTED"), repository.statusUpdates)
    }

    @Test
    fun `admin maintenance run answers a serializable summary`() = apiTest {
        val response = postJson("/api/v1/admin/maintenance/run", "{}", mapOf(HttpHeaders.Authorization to "Bearer $ADMIN_TOKEN"))
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"expiredShops\":2"))
    }
}
