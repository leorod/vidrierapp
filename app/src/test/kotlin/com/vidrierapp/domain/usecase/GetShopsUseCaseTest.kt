package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.ShopStatus
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetShopsUseCaseTest {

    private class FakeShopRepository : ShopRepository {
        var served: List<Shop>? = null
        val mockShops = listOf(
            Shop(
                id = "1",
                address = "Alberdi 100",
                neighborhood = "Mataderos",
                latitude = -34.65,
                longitude = -58.50,
                surfaceAreaM2 = 60,
                rentalPrice = "$600,000",
                topCategory = Category("cafeteria", "Cafetería"),
                totalVotes = 50,
                status = "SUFFICIENT"
            )
        )
        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> {
            return flowOf(Result.success(served ?: mockShops))
        }

        override suspend fun reportRented(shopId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException())

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> {
            return flowOf(Result.success(mockShops.first()))
        }
    }

    @Test
    fun `invoke GetShopsUseCase returns shops successfully`() = runTest {
        val repository = FakeShopRepository()
        val useGetShopsUseCase = GetShopsUseCase(repository)

        useGetShopsUseCase().collect { result ->
            val shops = result.getOrNull()
            assertEquals(1, shops?.size)
            assertEquals("Mataderos", shops?.first()?.neighborhood)
            assertEquals(50, shops?.first()?.totalVotes)
        }
    }

    @Test
    fun `invoke filters out rented shops and keeps under review ones`() = runTest {
        val repository = FakeShopRepository()
        val base = repository.mockShops.first()
        repository.served = listOf(
            base.copy(id = "active"),
            base.copy(id = "review", listingStatus = ShopStatus.UNDER_REVIEW),
            base.copy(id = "rented", listingStatus = ShopStatus.RENTED)
        )

        val shops = GetShopsUseCase(repository)().first().getOrThrow()

        assertEquals(listOf("active", "review"), shops.map { it.id })
    }

    @Test
    fun `invoke keeps a failure untouched`() = runTest {
        val failing = object : ShopRepository by FakeShopRepository() {
            override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> =
                flowOf(Result.failure(IllegalStateException("boom")))
        }

        assertEquals("boom", GetShopsUseCase(failing)().first().exceptionOrNull()?.message)
    }
}
