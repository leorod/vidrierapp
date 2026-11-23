package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetShopsUseCaseTest {

    private class FakeShopRepository : ShopRepository {
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
            return flowOf(Result.success(mockShops))
        }

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
}
