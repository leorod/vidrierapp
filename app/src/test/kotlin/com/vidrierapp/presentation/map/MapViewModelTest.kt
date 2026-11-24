package com.vidrierapp.presentation.map

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
        MapViewModel(GetShopsUseCase(repository), FakeLocationProvider(null))

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
}
