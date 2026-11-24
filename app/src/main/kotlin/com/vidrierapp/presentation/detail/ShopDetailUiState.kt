package com.vidrierapp.presentation.detail

import com.vidrierapp.domain.model.Shop

sealed interface ShopDetailUiState {
    data object Loading : ShopDetailUiState
    data class Success(
        val shop: Shop,
        val hasActivePass: Boolean = false,
        val trialAvailable: Boolean = true
    ) : ShopDetailUiState
    data class Error(val message: String) : ShopDetailUiState
}

data class ReportRentedUiState(
    val isDialogVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val alreadyReported: Boolean = false
)

enum class ReportRentedFeedback {
    Success,
    Failure,
    AlreadyReported
}
