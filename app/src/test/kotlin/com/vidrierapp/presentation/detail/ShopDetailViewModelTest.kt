package com.vidrierapp.presentation.detail

import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.ShopRepository
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopDetailViewModelTest {

    private class FakeShopRepository : ShopRepository {
        var result: Result<Shop> = Result.failure(IllegalStateException())
        val requestedIds = mutableListOf<String>()
        val flowsById = mutableMapOf<String, MutableSharedFlow<Result<Shop>>>()

        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> =
            flowOf(Result.failure(UnsupportedOperationException()))

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> {
            requestedIds.add(shopId)
            return flowsById[shopId] ?: flowOf(result)
        }
    }

    private fun shop(id: String) = Shop(
        id = id,
        address = "address $id",
        neighborhood = "neighborhood",
        latitude = -34.65,
        longitude = -58.50,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = null,
        totalVotes = 20
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadShop emits Success with active pass`() {
        val repository = FakeShopRepository().apply { result = Result.success(shop("1")) }
        val viewModel = ShopDetailViewModel(GetShopDetailUseCase(repository))

        viewModel.loadShop("1")

        assertEquals(
            ShopDetailUiState.Success(shop = shop("1"), hasActivePass = true),
            viewModel.uiState.value
        )
    }

    @Test
    fun `loadShop emits Error when loading fails`() {
        val repository = FakeShopRepository().apply {
            result = Result.failure(IllegalStateException("boom"))
        }
        val viewModel = ShopDetailViewModel(GetShopDetailUseCase(repository))

        viewModel.loadShop("1")

        assertEquals(ShopDetailUiState.Error("boom"), viewModel.uiState.value)
    }

    @Test
    fun `retry reloads the last shop after a failure`() {
        val repository = FakeShopRepository().apply {
            result = Result.failure(IllegalStateException("boom"))
        }
        val viewModel = ShopDetailViewModel(GetShopDetailUseCase(repository))
        viewModel.loadShop("1")

        repository.result = Result.success(shop("1"))
        viewModel.retry()

        assertTrue(viewModel.uiState.value is ShopDetailUiState.Success)
        assertEquals(listOf("1", "1"), repository.requestedIds)
    }

    @Test
    fun `retry without a previous load does nothing`() {
        val repository = FakeShopRepository()
        val viewModel = ShopDetailViewModel(GetShopDetailUseCase(repository))

        viewModel.retry()

        assertEquals(ShopDetailUiState.Loading, viewModel.uiState.value)
        assertTrue(repository.requestedIds.isEmpty())
    }

    @Test
    fun `loading another shop cancels the previous collection`() {
        val first = MutableSharedFlow<Result<Shop>>(extraBufferCapacity = 1)
        val second = MutableSharedFlow<Result<Shop>>(extraBufferCapacity = 1)
        val repository = FakeShopRepository().apply {
            flowsById["1"] = first
            flowsById["2"] = second
        }
        val viewModel = ShopDetailViewModel(GetShopDetailUseCase(repository))

        viewModel.loadShop("1")
        viewModel.loadShop("2")

        assertEquals(0, first.subscriptionCount.value)
        assertEquals(1, second.subscriptionCount.value)

        second.tryEmit(Result.success(shop("2")))
        first.tryEmit(Result.success(shop("1")))

        assertEquals(
            ShopDetailUiState.Success(shop = shop("2"), hasActivePass = true),
            viewModel.uiState.value
        )
    }
}
