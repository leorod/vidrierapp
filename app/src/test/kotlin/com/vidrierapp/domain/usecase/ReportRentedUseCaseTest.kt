package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRentedUseCaseTest {

    private class FakeShopRepository : ShopRepository {
        var result: Result<Unit> = Result.success(Unit)
        val reported = mutableListOf<String>()

        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> =
            flowOf(Result.failure(UnsupportedOperationException()))

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> =
            flowOf(Result.failure(UnsupportedOperationException()))

        override suspend fun reportRented(shopId: String): Result<Unit> {
            reported.add(shopId)
            return result
        }
    }

    private val shops = FakeShopRepository()
    private val reported = FakeReportedShopsRepository()
    private val useCase = ReportRentedUseCase(shops, reported)

    @Test
    fun `success reports to the repository and remembers the shop`() = runTest {
        val result = useCase("s1")

        assertTrue(result.isSuccess)
        assertEquals(listOf("s1"), shops.reported)
        assertEquals(setOf("s1"), reported.state.value)
    }

    @Test
    fun `failure is returned and the shop is not remembered`() = runTest {
        shops.result = Result.failure(IllegalStateException("boom"))

        val result = useCase("s1")

        assertTrue(result.isFailure)
        assertTrue(reported.state.value.isEmpty())
    }

    @Test
    fun `a failed report can be retried`() = runTest {
        shops.result = Result.failure(IllegalStateException("boom"))
        useCase("s1")
        shops.result = Result.success(Unit)

        assertTrue(useCase("s1").isSuccess)
        assertEquals(listOf("s1", "s1"), shops.reported)
    }

    @Test
    fun `second report of the same shop is rejected without calling the repository`() = runTest {
        useCase("s1")

        val result = useCase("s1")

        assertTrue(result.exceptionOrNull() is AlreadyReportedException)
        assertEquals(listOf("s1"), shops.reported)
    }

    @Test
    fun `different shops are reported independently`() = runTest {
        useCase("s1")

        assertTrue(useCase("s2").isSuccess)
        assertEquals(setOf("s1", "s2"), reported.state.value)
    }
}
