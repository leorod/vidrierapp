package com.vidrierapp.presentation.map

import com.vidrierapp.domain.location.NeighborhoodCatalog
import com.vidrierapp.domain.model.GeoPoint
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelLocationTest {

    private object EmptyShopRepository : ShopRepository {
        override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> =
            flowOf(Result.success(emptyList()))

        override fun getShopDetail(shopId: String): Flow<Result<Shop>> =
            flowOf(Result.failure(UnsupportedOperationException()))
    }

    private val user = GeoPoint(-34.65, -58.50)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(provider: FakeLocationProvider) =
        MapViewModel(GetShopsUseCase(EmptyShopRepository), provider)

    @Test
    fun `starts with unknown permission and no fallback`() {
        val state = viewModel(FakeLocationProvider(user)).locationState.value

        assertEquals(LocationPermission.Unknown, state.permission)
        assertFalse(state.showFallback)
        assertFalse(state.isGranted)
    }

    @Test
    fun `granted centers on user location`() {
        val provider = FakeLocationProvider(user)
        val viewModel = viewModel(provider)

        viewModel.onPermissionResult(true)

        val state = viewModel.locationState.value
        assertTrue(state.isGranted)
        assertEquals(user, state.userLocation)
        assertEquals(user, state.centerRequest?.point)
        assertFalse(state.showFallback)
        assertFalse(state.isLocating)
    }

    @Test
    fun `granted but unavailable shows fallback`() {
        val viewModel = viewModel(FakeLocationProvider(null))

        viewModel.onPermissionResult(true)

        val state = viewModel.locationState.value
        assertTrue(state.isUnavailable)
        assertTrue(state.showFallback)
        assertNull(state.userLocation)
        assertNull(state.centerRequest)
    }

    @Test
    fun `denied shows fallback and never queries the provider`() {
        val provider = FakeLocationProvider(user)
        val viewModel = viewModel(provider)

        viewModel.onPermissionResult(false)
        viewModel.recenterOnUser()

        val state = viewModel.locationState.value
        assertEquals(LocationPermission.Denied, state.permission)
        assertTrue(state.showFallback)
        assertFalse(state.isGranted)
        assertEquals(0, provider.calls)
    }

    @Test
    fun `recenter issues a new center request`() {
        val provider = FakeLocationProvider(user)
        val viewModel = viewModel(provider)
        viewModel.onPermissionResult(true)
        val first = viewModel.locationState.value.centerRequest

        viewModel.recenterOnUser()

        val second = viewModel.locationState.value.centerRequest
        assertNotNull(second)
        assertNotEquals(first?.id, second?.id)
        assertEquals(2, provider.calls)
    }

    @Test
    fun `manual pick sets position and leaves picking mode`() {
        val viewModel = viewModel(FakeLocationProvider(null))
        viewModel.onPermissionResult(false)
        val point = GeoPoint(-34.64, -58.59)

        viewModel.startManualPick()
        assertTrue(viewModel.locationState.value.isPickingManually)
        viewModel.onManualLocationSelected(point)

        val state = viewModel.locationState.value
        assertFalse(state.isPickingManually)
        assertEquals(point, state.manualLocation)
        assertEquals(point, state.centerRequest?.point)
    }

    @Test
    fun `cancel pick leaves picking mode without position`() {
        val viewModel = viewModel(FakeLocationProvider(null))
        viewModel.onPermissionResult(false)

        viewModel.startManualPick()
        viewModel.cancelManualPick()

        val state = viewModel.locationState.value
        assertFalse(state.isPickingManually)
        assertNull(state.manualLocation)
    }

    @Test
    fun `search suggests neighborhoods ignoring case and accents`() {
        val viewModel = viewModel(FakeLocationProvider(null))
        viewModel.onPermissionResult(false)

        viewModel.onSearchQueryChange("moron")

        assertEquals(listOf("Morón"), viewModel.locationState.value.suggestions.map { it.name })
        viewModel.onSearchQueryChange("")
        assertTrue(viewModel.locationState.value.suggestions.isEmpty())
    }

    @Test
    fun `selecting a neighborhood centers the map on it`() {
        val viewModel = viewModel(FakeLocationProvider(null))
        viewModel.onPermissionResult(false)
        val haedo = NeighborhoodCatalog.search("Haedo").single()

        viewModel.onNeighborhoodSelected(haedo)

        val state = viewModel.locationState.value
        assertEquals(haedo.center, state.manualLocation)
        assertEquals(haedo.center, state.centerRequest?.point)
        assertEquals("Haedo", state.searchQuery)
        assertTrue(state.suggestions.isEmpty())
    }

    @Test
    fun `granting after manual selection clears the manual position`() {
        val viewModel = viewModel(FakeLocationProvider(user))
        viewModel.onPermissionResult(false)
        viewModel.onManualLocationSelected(GeoPoint(-34.6, -58.4))

        viewModel.onPermissionResult(true)

        val state = viewModel.locationState.value
        assertNull(state.manualLocation)
        assertEquals(user, state.centerRequest?.point)
    }
}
