package com.vidrierapp.presentation.map

import com.vidrierapp.domain.model.CachedShop
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.network.FakeNetworkMonitor
import com.vidrierapp.domain.repository.ShopRepository
import com.vidrierapp.domain.usecase.FakeCachedShopRepository
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import com.vidrierapp.domain.usecase.GetShopsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class MapViewModelOfflineTest {

    private class FakeShopRepository(var result: Result<List<Shop>>) : ShopRepository {
        var calls = 0

        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> {
            calls++
            return flowOf(result)
        }

        override suspend fun reportRented(shopId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException())

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> =
            flowOf(Result.failure(UnsupportedOperationException()))
    }

    private val network = FakeNetworkMonitor()

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

    private fun viewModel(
        repository: FakeShopRepository,
        cached: List<CachedShop> = emptyList()
    ) = MapViewModel(
        GetShopsUseCase(repository),
        FakeLocationProvider(null),
        GetCachedShopsUseCase(FakeCachedShopRepository(cached)),
        network
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
    fun `offline failure with nothing cached shows offline error`() {
        network.online.value = false
        val vm = viewModel(FakeShopRepository(Result.failure(java.io.IOException("down"))))

        assertEquals(MapUiState.Error("down", isOffline = true), vm.uiState.value)
    }

    @Test
    fun `online failure with nothing cached shows plain error`() {
        val vm = viewModel(FakeShopRepository(Result.failure(IllegalStateException("boom"))))

        assertEquals(MapUiState.Error("boom", isOffline = false), vm.uiState.value)
    }

    @Test
    fun `failure falls back to cached shops instead of blank error`() {
        network.online.value = false
        val vm = viewModel(
            FakeShopRepository(Result.failure(java.io.IOException("down"))),
            cached = listOf(CachedShop(shop("a"), 1L), CachedShop(shop("b"), 2L))
        )

        val state = vm.uiState.value as MapUiState.Success
        assertEquals(listOf("a", "b"), state.shops.map { it.id })
    }

    @Test
    fun `retry re-executes the request and recovers`() {
        network.online.value = false
        val repository = FakeShopRepository(Result.failure(java.io.IOException("down")))
        val vm = viewModel(repository)
        assertTrue(vm.uiState.value is MapUiState.Error)

        network.online.value = true
        repository.result = Result.success(listOf(shop("a")))
        vm.retry()

        assertEquals(2, repository.calls)
        assertEquals(listOf("a"), (vm.uiState.value as MapUiState.Success).shops.map { it.id })
    }
}
