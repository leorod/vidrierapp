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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelCategoryFilterTest {

    private class CountingRepository(private val shops: List<Shop>) : ShopRepository {
        var calls = 0

        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> {
            calls++
            return flowOf(Result.success(shops))
        }

        override suspend fun reportRented(shopId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException())

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> =
            flowOf(Result.failure(UnsupportedOperationException()))
    }

    private val cafe = Category("cafe", "Cafeteria")
    private val bakery = Category("bakery", "Panaderia")
    private val greengrocer = Category("greengrocer", "Verduleria")

    private fun shop(id: String, category: Category?) = Shop(
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

    private val shops = listOf(
        shop("1", cafe),
        shop("2", bakery),
        shop("3", cafe),
        shop("4", null)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: CountingRepository = CountingRepository(shops)) =
        MapViewModel(GetShopsUseCase(repository), FakeLocationProvider(null), GetCachedShopsUseCase(FakeCachedShopRepository()), FakeNetworkMonitor())

    private fun MapViewModel.success() = uiState.value as MapUiState.Success

    @Test
    fun `all shops are visible with no filter`() {
        val state = viewModel().success()

        assertEquals(shops, state.shops)
        assertNull(state.selectedCategoryFilter)
    }

    @Test
    fun `categories are derived from the shops sorted by name without duplicates`() {
        assertEquals(listOf(cafe, bakery).sortedBy { it.name }, viewModel().success().categories)
    }

    @Test
    fun `selecting a category shows only shops led by it without a new request`() {
        val repository = CountingRepository(shops)
        val vm = viewModel(repository)

        vm.selectCategory("cafe")

        assertEquals(listOf("1", "3"), vm.success().shops.map { it.id })
        assertEquals("cafe", vm.success().selectedCategoryFilter)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `selecting null restores every shop`() {
        val vm = viewModel()
        vm.selectCategory("cafe")

        vm.selectCategory(null)

        assertEquals(shops, vm.success().shops)
        assertNull(vm.success().selectedCategoryFilter)
    }

    @Test
    fun `a category with no matching shops yields an empty list`() {
        val vm = viewModel()

        vm.selectCategory(greengrocer.id)

        assertEquals(emptyList<Shop>(), vm.success().shops)
        assertEquals("greengrocer", vm.success().selectedCategoryFilter)
    }

    @Test
    fun `selected shop is cleared when it is filtered out`() {
        val vm = viewModel()
        vm.selectShop(shops[1])

        vm.selectCategory("cafe")

        assertNull(vm.success().selectedShop)
    }

    @Test
    fun `selected shop is kept when it still matches`() {
        val vm = viewModel()
        vm.selectShop(shops[0])

        vm.selectCategory("cafe")

        assertEquals(shops[0], vm.success().selectedShop)
    }

    @Test
    fun `loadShops with a filter applies it to the loaded shops`() {
        val vm = viewModel()

        vm.loadShops("bakery")

        assertEquals(listOf("2"), vm.success().shops.map { it.id })
        assertEquals("bakery", vm.success().selectedCategoryFilter)
    }

    @Test
    fun `selectCategory is a no-op before shops are loaded`() {
        val vm = MapViewModel(
            GetShopsUseCase(object : ShopRepository {
                override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> =
                    flowOf(Result.failure(IllegalStateException("boom")))

                override suspend fun reportRented(shopId: String): Result<Unit> =
                    Result.failure(UnsupportedOperationException())

                override fun getShopDetail(shopId: String): Flow<Result<Shop>> =
                    flowOf(Result.failure(UnsupportedOperationException()))
            }),
            FakeLocationProvider(null),
            GetCachedShopsUseCase(FakeCachedShopRepository()),
            FakeNetworkMonitor()
        )

        vm.selectCategory("cafe")

        assertEquals(MapUiState.Error("boom"), vm.uiState.value)
    }
}
