package com.vidrierapp.presentation.map

import com.vidrierapp.domain.model.Shop

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Success(
        val shops: List<Shop> = emptyList(),
        val selectedCategoryFilter: String? = null,
        val selectedShop: Shop? = null
    ) : MapUiState
    data class Error(val message: String) : MapUiState
}
