package com.vidrierapp.presentation.detail

import com.vidrierapp.domain.model.Shop

sealed interface ShopDetailUiState {
    data object Loading : ShopDetailUiState
    data class Success(
        val shop: Shop,
        val hasActivePass: Boolean = false
    ) : ShopDetailUiState
    data class Error(val message: String) : ShopDetailUiState
}
