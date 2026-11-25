package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.CachedShop
import com.vidrierapp.domain.model.Shop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCachedShopsUseCaseTest {

    private fun cached(id: String, savedAt: Long) = CachedShop(
        shop = Shop(
            id = id,
            address = "address $id",
            neighborhood = "neighborhood",
            latitude = 0.0,
            longitude = 0.0,
            surfaceAreaM2 = 50,
            rentalPrice = "100",
            topCategory = null
        ),
        savedAt = savedAt
    )

    @Test
    fun `invoke returns what the repository holds in the same order`() = runTest {
        val entries = listOf(cached("new", 200L), cached("old", 100L))

        val result = GetCachedShopsUseCase(FakeCachedShopRepository(entries))().first()

        assertEquals(entries, result)
    }

    @Test
    fun `invoke returns an empty list when nothing is cached`() = runTest {
        val result = GetCachedShopsUseCase(FakeCachedShopRepository())().first()

        assertTrue(result.isEmpty())
    }
}
