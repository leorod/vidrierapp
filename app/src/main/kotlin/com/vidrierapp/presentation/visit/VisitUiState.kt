package com.vidrierapp.presentation.visit

import com.vidrierapp.domain.model.Visit
import com.vidrierapp.presentation.navigation.NO_SHOP_ID

data class VisitUiState(
    val shopId: String = NO_SHOP_ID,
    val manualAddress: String = "",
    val notes: String = "",
    val pedestrianCount: Int = 0,
    val photoUris: List<String> = emptyList(),
    val pastVisits: List<Visit> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val showAddressError: Boolean = false,
    val saveFailed: Boolean = false
) {
    val requiresManualAddress: Boolean get() = shopId == NO_SHOP_ID
}
