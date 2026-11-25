package com.vidrierapp.data.repository

import com.vidrierapp.data.local.dao.CachedLocalDao
import com.vidrierapp.data.local.entity.CachedLocalEntity
import com.vidrierapp.data.remote.api.VidrierAppApiService
import com.vidrierapp.data.remote.dto.ReportRentedResponseDto
import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RetrofitShopRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val day = 24L * 60 * 60 * 1000
    private val hour = 60L * 60 * 1000
    private var now = 10 * day

    private class FakeDao : CachedLocalDao {
        val rows = MutableStateFlow<Map<String, CachedLocalEntity>>(emptyMap())
        var failOnUpsert = false

        override suspend fun upsert(entry: CachedLocalEntity) {
            if (failOnUpsert) throw IllegalStateException("disk full")
            rows.value = rows.value + (entry.shopId to entry)
        }

        override suspend fun getValid(shopId: String, cutoff: Long): CachedLocalEntity? =
            rows.value[shopId]?.takeIf { it.cachedAt > cutoff }

        override fun getCachedShops(cutoff: Long): Flow<List<CachedLocalEntity>> =
            rows.map { all -> all.values.filter { it.cachedAt > cutoff }.sortedByDescending { it.cachedAt } }

        override suspend fun deleteExpired(cutoff: Long): Int {
            val expired = rows.value.filterValues { it.cachedAt <= cutoff }
            rows.value = rows.value - expired.keys
            return expired.size
        }
    }

    private class FakeApi : VidrierAppApiService {
        var online = true
        var detail: ShopDto = dto("s1")
        var calls = 0
        val reported = mutableListOf<String>()

        val requestedCategories = mutableListOf<String?>()
        var shops: List<ShopDto> = emptyList()

        override suspend fun getShops(category: String?): List<ShopDto> {
            requestedCategories += category
            if (!online) throw IOException("offline")
            return shops
        }

        override suspend fun getShopDetail(id: String): ShopDto {
            calls++
            if (!online) throw IOException("offline")
            return detail.copy(id = id)
        }

        override suspend fun reportRented(id: String): ReportRentedResponseDto {
            if (!online) throw IOException("offline")
            reported += id
            return ReportRentedResponseDto(shopId = id, reportCount = reported.size)
        }

    }

    private lateinit var dao: FakeDao
    private lateinit var api: FakeApi
    private lateinit var repository: RetrofitShopRepository

    @Before
    fun setUp() {
        dao = FakeDao()
        api = FakeApi()
        repository = RetrofitShopRepository(api, dao, json) { now }
    }

    @Test
    fun `network success returns remote shop and writes cache`() = runTest {
        val result = repository.getShopDetail("s1").first()

        assertEquals("s1", result.getOrThrow().id)
        assertEquals(now, dao.rows.value.getValue("s1").cachedAt)
    }

    @Test
    fun `network success refreshes savedAt of existing entry`() = runTest {
        repository.getShopDetail("s1").first()
        now += 2 * day
        repository.getShopDetail("s1").first()

        assertEquals(now, dao.rows.value.getValue("s1").cachedAt)
    }

    @Test
    fun `offline returns cached shop when entry is valid`() = runTest {
        api.detail = dto("s1", address = "Calle Cache 1")
        repository.getShopDetail("s1").first()
        api.online = false
        now += 6 * day + 23 * hour

        val result = repository.getShopDetail("s1").first()

        assertEquals("Calle Cache 1", result.getOrThrow().address)
    }

    @Test
    fun `offline fails and purges entry when exactly seven days old`() = runTest {
        repository.getShopDetail("s1").first()
        api.online = false
        now += 7 * day

        val result = repository.getShopDetail("s1").first()

        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `offline returns cached shop one millisecond before seven days`() = runTest {
        api.detail = dto("s1", address = "Calle Cache 1")
        repository.getShopDetail("s1").first()
        api.online = false
        now += 7 * day - 1

        val result = repository.getShopDetail("s1").first()

        assertEquals("Calle Cache 1", result.getOrThrow().address)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `offline fails and purges entry one millisecond after seven days`() = runTest {
        repository.getShopDetail("s1").first()
        api.online = false
        now += 7 * day + 1

        val result = repository.getShopDetail("s1").first()

        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `offline purges expired entries of other shops while serving a valid one`() = runTest {
        repository.getShopDetail("stale").first()
        now += 5 * day
        repository.getShopDetail("fresh").first()
        now += 3 * day
        api.online = false

        val result = repository.getShopDetail("fresh").first()

        assertEquals("fresh", result.getOrThrow().id)
        assertEquals(setOf("fresh"), dao.rows.value.keys)
    }

    @Test
    fun `an offline read does not extend the cache lifetime`() = runTest {
        repository.getShopDetail("s1").first()
        api.online = false
        now += 3 * day
        assertTrue(repository.getShopDetail("s1").first().isSuccess)
        now += 4 * day

        val result = repository.getShopDetail("s1").first()

        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `getShops maps the dto list and forwards the category filter`() = runTest {
        api.shops = listOf(dto("a"), dto("b"))

        val result = repository.getShops("cafeteria").first()

        assertEquals(listOf("a", "b"), result.getOrThrow().map { it.id })
        assertEquals(listOf<String?>("cafeteria"), api.requestedCategories)
    }

    @Test
    fun `getShops without filter sends no category`() = runTest {
        repository.getShops(null).first()

        assertEquals(listOf<String?>(null), api.requestedCategories)
    }

    @Test
    fun `getShops returns a failure when the network is down`() = runTest {
        api.online = false

        val result = repository.getShops(null).first()

        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `offline without cache returns the network failure`() = runTest {
        api.online = false

        val result = repository.getShopDetail("s1").first()

        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `cache write failure does not break a successful network read`() = runTest {
        dao.failOnUpsert = true

        val result = repository.getShopDetail("s1").first()

        assertEquals("s1", result.getOrThrow().id)
    }

    @Test
    fun `getCachedShops returns only non expired entries with savedAt newest first`() = runTest {
        repository.getShopDetail("old").first()
        now += 3 * day
        repository.getShopDetail("mid").first()
        now += 4 * day
        repository.getShopDetail("new").first()

        val cached = GetCachedShopsUseCase(repository)().first()

        assertEquals(listOf("new", "mid"), cached.map { it.shop.id })
        assertEquals(now, cached.first().savedAt)
        assertEquals(now - 4 * day, cached[1].savedAt)
    }

    @Test
    fun `getCachedShops keeps entry at 6d23h and drops it at 7d`() = runTest {
        repository.getShopDetail("s1").first()
        val savedAt = now

        now = savedAt + 6 * day + 23 * hour
        assertEquals(1, repository.getCachedShops().first().size)

        now = savedAt + 7 * day
        assertTrue(repository.getCachedShops().first().isEmpty())
    }

    @Test
    fun `purgeExpiredCache removes only entries at or past seven days`() = runTest {
        dao.upsert(CachedLocalEntity("exact", payload("exact"), now - 7 * day))
        dao.upsert(CachedLocalEntity("older", payload("older"), now - 9 * day))
        dao.upsert(CachedLocalEntity("fresh", payload("fresh"), now - 6 * day - 23 * hour))

        repository.purgeExpiredCache()

        assertEquals(setOf("fresh"), dao.rows.value.keys)
        assertNull(dao.getValid("exact", now - 7 * day))
    }

    private fun payload(id: String): String = json.encodeToString(ShopDto.serializer(), dto(id))

    companion object {
        fun dto(id: String, address: String = "Alberdi 100") = ShopDto(
            id = id,
            address = address,
            neighborhood = "Mataderos",
            latitude = -34.65,
            longitude = -58.50,
            surfaceAreaM2 = 60,
            rentalPrice = "$600,000",
            totalVotes = 20
        )
    }

    @Test
    fun `reportRented succeeds when the api accepts the report`() = runTest {
        val result = repository.reportRented("s1")

        assertTrue(result.isSuccess)
        assertEquals(listOf("s1"), api.reported)
    }

    @Test
    fun `reportRented wraps api failures`() = runTest {
        api.online = false

        val result = repository.reportRented("s1")

        assertTrue(result.exceptionOrNull() is IOException)
    }
}
