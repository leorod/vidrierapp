package com.vidrierapp.presentation.map

import com.vidrierapp.domain.location.Neighborhood
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.GeoPoint
import com.vidrierapp.domain.model.Shop

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Success(
        val shops: List<Shop> = emptyList(),
        val categories: List<Category> = emptyList(),
        val selectedCategoryFilter: String? = null,
        val selectedShop: Shop? = null
    ) : MapUiState
    data class Error(val message: String) : MapUiState
}

enum class LocationPermission { Unknown, Granted, Denied }

data class CenterRequest(val id: Int, val point: GeoPoint)

data class LocationUiState(
    val permission: LocationPermission = LocationPermission.Unknown,
    val isLocating: Boolean = false,
    val isUnavailable: Boolean = false,
    val userLocation: GeoPoint? = null,
    val manualLocation: GeoPoint? = null,
    val isPickingManually: Boolean = false,
    val searchQuery: String = "",
    val suggestions: List<Neighborhood> = emptyList(),
    val centerRequest: CenterRequest? = null
) {
    val isGranted: Boolean get() = permission == LocationPermission.Granted
    val showFallback: Boolean
        get() = permission == LocationPermission.Denied || (isGranted && isUnavailable)
}
