package com.vidrierapp.presentation.map

import com.vidrierapp.domain.network.FakeNetworkMonitor
import com.vidrierapp.domain.usecase.FakeCachedShopRepository
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.ShopRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private class FakeShopRepository(
        var result: Result<List<Shop>>
    ) : ShopRepository {
        var requestedFilters = mutableListOf<String?>()

        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> {
            requestedFilters.add(categoryFilter)
            return flowOf(result)
        }

        override suspend fun reportRented(shopId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException())

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> =
            flowOf(Result.failure(UnsupportedOperationException()))
    }

    private fun shop(id: String, category: Category? = null) = Shop(
        id = id,
        address = "address $id",
        neighborhood = "neighborhood",
        latitude = -34.65,
        longitude = -58.50,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = category,
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

    private fun createViewModel(repository: FakeShopRepository) =
        MapViewModel(GetShopsUseCase(repository), FakeLocationProvider(null), GetCachedShopsUseCase(FakeCachedShopRepository()), FakeNetworkMonitor())

    @Test
    fun `loads shops on init`() {
        val shops = listOf(shop("1"), shop("2"))

        val viewModel = createViewModel(FakeShopRepository(Result.success(shops)))

        assertEquals(MapUiState.Success(shops = shops), viewModel.uiState.value)
    }

    @Test
    fun `emits Error when loading fails`() {
        val viewModel = createViewModel(
            FakeShopRepository(Result.failure(IllegalStateException("boom")))
        )

        assertEquals(MapUiState.Error("boom"), viewModel.uiState.value)
    }

    @Test
    fun `selectShop updates selectedShop`() {
        val shops = listOf(shop("1"))
        val viewModel = createViewModel(FakeShopRepository(Result.success(shops)))

        viewModel.selectShop(shops.first())

        assertEquals(shops.first(), (viewModel.uiState.value as MapUiState.Success).selectedShop)
    }

    @Test
    fun `selectShop with null clears selection`() {
        val shops = listOf(shop("1"))
        val viewModel = createViewModel(FakeShopRepository(Result.success(shops)))
        viewModel.selectShop(shops.first())

        viewModel.selectShop(null)

        assertNull((viewModel.uiState.value as MapUiState.Success).selectedShop)
    }

    @Test
    fun `selectShop is a no-op when state is not Success`() {
        val viewModel = createViewModel(
            FakeShopRepository(Result.failure(IllegalStateException("boom")))
        )

        viewModel.selectShop(shop("1"))

        assertTrue(viewModel.uiState.value is MapUiState.Error)
    }

    @Test
    fun `retry reloads shops after a failure`() {
        val shops = listOf(shop("1"))
        val repository = FakeShopRepository(Result.failure(IllegalStateException("boom")))
        val viewModel = createViewModel(repository)

        repository.result = Result.success(shops)
        viewModel.retry()

        assertEquals(MapUiState.Success(shops = shops), viewModel.uiState.value)
        assertEquals(2, repository.requestedFilters.size)
    }

    @Test
    fun `retry keeps the selected category without sending it to the server`() {
        val cafe = Category("cafe", "Cafeteria")
        val repository = FakeShopRepository(Result.success(listOf(shop("1", cafe))))
        val viewModel = createViewModel(repository)
        viewModel.selectCategory("cafe")

        viewModel.retry()

        assertEquals("cafe", (viewModel.uiState.value as MapUiState.Success).selectedCategoryFilter)
        assertEquals(listOf<String?>(null, null), repository.requestedFilters)
    }

    @Test
    fun `refresh updates vote counts and keeps the selected shop`() {
        val repository = FakeShopRepository(Result.success(listOf(shop("1").copy(totalVotes = 0))))
        val viewModel = createViewModel(repository)
        viewModel.selectShop((viewModel.uiState.value as MapUiState.Success).shops.first())

        repository.result = Result.success(listOf(shop("1").copy(totalVotes = 1)))
        viewModel.refresh()

        val state = viewModel.uiState.value as MapUiState.Success
        assertEquals(1, state.shops.first().totalVotes)
        assertEquals(1, state.selectedShop?.totalVotes)
    }

    @Test
    fun `refresh keeps the current shops when the request fails`() {
        val shops = listOf(shop("1"))
        val repository = FakeShopRepository(Result.success(shops))
        val viewModel = createViewModel(repository)

        repository.result = Result.failure(IllegalStateException("boom"))
        viewModel.refresh()

        assertEquals(MapUiState.Success(shops = shops), viewModel.uiState.value)
    }
}
