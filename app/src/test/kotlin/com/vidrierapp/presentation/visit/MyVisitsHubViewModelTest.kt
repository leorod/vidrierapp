package com.vidrierapp.presentation.visit

import com.vidrierapp.domain.model.CachedShop
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.CachedShopRepository
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import com.vidrierapp.domain.repository.VisitRepository
import com.vidrierapp.domain.usecase.GetAllVisitsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
class MyVisitsHubViewModelTest {

    private class FakeVisitRepository(
        private val allVisits: Flow<List<Visit>>
    ) : VisitRepository {
        override fun getVisitsForShop(shopId: String): Flow<List<Visit>> = flowOf(emptyList())
        override fun getAllVisits(): Flow<List<Visit>> = allVisits
        override suspend fun saveVisit(visit: Visit): Result<Unit> = Result.success(Unit)
    }

    private fun visit(id: String, timestamp: Long) = Visit(
        id = id,
        shopId = "shop-$id",
        timestamp = timestamp,
        notes = "nota $id",
        pedestrianCount = 3
    )

    private class FakeCachedShopRepository(
        private val cached: Flow<List<CachedShop>>
    ) : CachedShopRepository {
        override fun getCachedShops(): Flow<List<CachedShop>> = cached
        override suspend fun purgeExpiredCache() = Unit
    }

    private fun shop(id: String, address: String = "Calle $id 100") = Shop(
        id = id,
        address = address,
        neighborhood = "Palermo",
        latitude = 0.0,
        longitude = 0.0,
        surfaceAreaM2 = 40,
        rentalPrice = "100",
        topCategory = null
    )

    private fun createViewModel(
        visits: Flow<List<Visit>>,
        cached: Flow<List<CachedShop>> = flowOf(emptyList()),
        now: Long = 0L
    ) = MyVisitsHubViewModel(
        GetAllVisitsUseCase(FakeVisitRepository(visits)),
        GetCachedShopsUseCase(FakeCachedShopRepository(cached)),
        { now }
    )

    private val day = MILLIS_PER_DAY

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Success with visits in the given order`() {
        val visits = listOf(visit("2", 200L), visit("1", 100L))

        val viewModel = createViewModel(flowOf(visits))

        assertEquals(visits, (viewModel.uiState.value as MyVisitsHubUiState.Success).visits)
    }

    @Test
    fun `emits Success with empty list when there are no visits`() {
        val viewModel = createViewModel(flowOf(emptyList()))

        assertEquals(MyVisitsHubUiState.Success(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun `emits Error when the flow throws`() {
        val failing = flow<List<Visit>> { throw IllegalStateException("boom") }

        val viewModel = createViewModel(failing)

        val state = viewModel.uiState.value
        assertTrue(state is MyVisitsHubUiState.Error)
        assertEquals("boom", (state as MyVisitsHubUiState.Error).message)
    }

    private fun success(vm: MyVisitsHubViewModel) = vm.uiState.value as MyVisitsHubUiState.Success

    @Test
    fun `visits are sorted newest first`() {
        val viewModel = createViewModel(flowOf(listOf(visit("1", 100L), visit("3", 300L), visit("2", 200L))))

        assertEquals(listOf("3", "2", "1"), success(viewModel).visits.map { it.id })
    }

    @Test
    fun `combines visits and cached shops and resolves names`() {
        val cached = listOf(CachedShop(shop("shop-1", "Av. Santa Fe 1234"), savedAt = 0L))
        val viewModel = createViewModel(flowOf(listOf(visit("1", 100L))), flowOf(cached), now = 2 * day)

        val state = success(viewModel)

        assertEquals(1, state.visits.size)
        assertEquals("Av. Santa Fe 1234", state.shopNames["shop-1"])
        assertEquals(null, state.shopNames["shop-x"])
        assertEquals(
            listOf(SavedShopItem("shop-1", "Av. Santa Fe 1234", "Palermo", 2)),
            state.savedShops
        )
    }

    @Test
    fun `days ago boundaries`() {
        val saved = 10 * day
        val cached = listOf(CachedShop(shop("a"), saved))
        fun daysAt(now: Long) =
            success(createViewModel(flowOf(emptyList()), flowOf(cached), now)).savedShops.single().daysAgo

        assertEquals(0, daysAt(saved))
        assertEquals(0, daysAt(saved + day - 1))
        assertEquals(1, daysAt(saved + day))
        assertEquals(6, daysAt(saved + 7 * day - 1))
        assertEquals(7, daysAt(saved + 7 * day))
        assertEquals(0, daysAt(saved - 5 * day))
    }

    @Test
    fun `saved shops are sorted by most recently saved`() {
        val cached = listOf(CachedShop(shop("old"), 1L), CachedShop(shop("new"), 5L))

        val viewModel = createViewModel(flowOf(emptyList()), flowOf(cached), now = 10L)

        assertEquals(listOf("new", "old"), success(viewModel).savedShops.map { it.shopId })
    }

    @Test
    fun `empty visits and empty cache give empty success`() {
        val state = success(createViewModel(flowOf(emptyList())))

        assertTrue(state.visits.isEmpty())
        assertTrue(state.savedShops.isEmpty())
    }

    @Test
    fun `cached shops failure emits Error`() {
        val failing = flow<List<CachedShop>> { throw IllegalStateException("db") }

        val viewModel = createViewModel(flowOf(emptyList()), failing)

        assertEquals("db", (viewModel.uiState.value as MyVisitsHubUiState.Error).message)
    }

    @Test
    fun `reload picks up changes after a refresh`() {
        val cached = MutableStateFlow(listOf(CachedShop(shop("a"), 0L)))
        val viewModel = createViewModel(flowOf(emptyList()), cached)

        cached.value = cached.value + CachedShop(shop("b"), 0L)
        viewModel.loadVisits()

        assertEquals(2, success(viewModel).savedShops.size)
    }
}
