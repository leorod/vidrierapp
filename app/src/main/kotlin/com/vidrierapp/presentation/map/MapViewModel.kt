package com.vidrierapp.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.location.LocationProvider
import com.vidrierapp.domain.location.Neighborhood
import com.vidrierapp.domain.location.NeighborhoodCatalog
import com.vidrierapp.domain.model.GeoPoint
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.usecase.GetShopsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val getShopsUseCase: GetShopsUseCase,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _locationState = MutableStateFlow(LocationUiState())
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    private var centerRequestId = 0

    private var currentFilter: String? = null

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
            getShopsUseCase(categoryFilter).collect { result ->
                result.fold(
                    onSuccess = { shops ->
                        _uiState.value = MapUiState.Success(
                            shops = shops,
                            selectedCategoryFilter = categoryFilter
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = MapUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }

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
