package com.vidrierapp.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.location.LocationProvider
import com.vidrierapp.domain.location.Neighborhood
import com.vidrierapp.domain.location.NeighborhoodCatalog
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.GeoPoint
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.network.NetworkMonitor
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import com.vidrierapp.domain.usecase.GetShopsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val getShopsUseCase: GetShopsUseCase,
    private val locationProvider: LocationProvider,
    private val getCachedShopsUseCase: GetCachedShopsUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _locationState = MutableStateFlow(LocationUiState())
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    private var centerRequestId = 0

    private var currentFilter: String? = null
    private var allShops: List<Shop> = emptyList()

    init {
        loadShops()
    }

    fun retry() {
        loadShops(currentFilter)
    }

    fun loadShops(categoryFilter: String? = null) {
        currentFilter = categoryFilter
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            getShopsUseCase().collect { result ->
                result.fold(
                    onSuccess = { shops ->
                        allShops = shops
                        val categories = deriveCategories(shops)
                        _uiState.value = MapUiState.Success(
                            shops = applyFilter(shops, categoryFilter),
                            categories = categories,
                            selectedCategoryFilter = categoryFilter
                        )
                    },
                    onFailure = { error ->
                        showCachedOrError(error, categoryFilter)
                    }
                )
            }
        }
    }

    fun refresh() {
        if (_uiState.value !is MapUiState.Success) return
        viewModelScope.launch {
            val shops = getShopsUseCase().first().getOrNull() ?: return@launch
            val current = _uiState.value as? MapUiState.Success ?: return@launch
            allShops = shops
            val visible = applyFilter(shops, currentFilter)
            _uiState.value = current.copy(
                shops = visible,
                categories = deriveCategories(shops),
                isFromCache = false,
                selectedShop = current.selectedShop?.let { selected ->
                    visible.firstOrNull { it.id == selected.id }
                }
            )
        }
    }

    private suspend fun showCachedOrError(error: Throwable, categoryFilter: String?) {
        val cached = getCachedShopsUseCase().first().map { it.shop }
        if (cached.isNotEmpty()) {
            allShops = cached
            _uiState.value = MapUiState.Success(
                shops = applyFilter(cached, categoryFilter),
                categories = deriveCategories(cached),
                selectedCategoryFilter = categoryFilter,
                isFromCache = true
            )
        } else {
            _uiState.value = MapUiState.Error(
                message = error.message.orEmpty(),
                isOffline = !networkMonitor.isOnline.first()
            )
        }
    }

    fun selectCategory(categoryId: String?) {
        val current = _uiState.value as? MapUiState.Success ?: return
        currentFilter = categoryId
        val visible = applyFilter(allShops, categoryId)
        _uiState.value = current.copy(
            shops = visible,
            selectedCategoryFilter = categoryId,
            selectedShop = current.selectedShop?.takeIf { selected ->
                visible.any { it.id == selected.id }
            }
        )
    }

    private fun applyFilter(shops: List<Shop>, categoryId: String?): List<Shop> =
        if (categoryId == null) shops else shops.filter { it.topCategory?.id == categoryId }

    private fun deriveCategories(shops: List<Shop>): List<Category> =
        shops.mapNotNull { it.topCategory }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

    fun selectShop(shop: Shop?) {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(selectedShop = shop)
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _locationState.update { it.copy(permission = LocationPermission.Granted) }
            locate()
        } else {
            _locationState.update {
                it.copy(permission = LocationPermission.Denied, isLocating = false)
            }
        }
    }

    fun recenterOnUser() {
        if (_locationState.value.isGranted) locate()
    }

    fun startManualPick() {
        _locationState.update { it.copy(isPickingManually = true) }
    }

    fun cancelManualPick() {
        _locationState.update { it.copy(isPickingManually = false) }
    }

    fun onManualLocationSelected(point: GeoPoint) {
        _locationState.update {
            it.copy(
                manualLocation = point,
                isPickingManually = false,
                centerRequest = nextCenterRequest(point)
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _locationState.update {
            it.copy(searchQuery = query, suggestions = NeighborhoodCatalog.search(query))
        }
    }

    fun onNeighborhoodSelected(neighborhood: Neighborhood) {
        _locationState.update {
            it.copy(
                searchQuery = neighborhood.name,
                suggestions = emptyList(),
                manualLocation = neighborhood.center,
                isPickingManually = false,
                centerRequest = nextCenterRequest(neighborhood.center)
            )
        }
    }

    private fun locate() {
        viewModelScope.launch {
            _locationState.update { it.copy(isLocating = true) }
            val point = locationProvider.currentLocation()
            _locationState.update {
                if (point == null) {
                    it.copy(isLocating = false, isUnavailable = true)
                } else {
                    it.copy(
                        isLocating = false,
                        isUnavailable = false,
                        userLocation = point,
                        manualLocation = null,
                        centerRequest = nextCenterRequest(point)
                    )
                }
            }
        }
    }

    private fun nextCenterRequest(point: GeoPoint): CenterRequest {
        centerRequestId += 1
        return CenterRequest(centerRequestId, point)
    }
}
