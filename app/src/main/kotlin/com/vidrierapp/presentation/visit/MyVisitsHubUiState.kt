package com.vidrierapp.presentation.visit

import com.vidrierapp.domain.model.Visit

data class SavedShopItem(
    val shopId: String,
    val address: String,
    val neighborhood: String,
    val daysAgo: Int
)

sealed interface MyVisitsHubUiState {
    data object Loading : MyVisitsHubUiState
    data class Success(
        val visits: List<Visit> = emptyList(),
        val savedShops: List<SavedShopItem> = emptyList(),
        val shopNames: Map<String, String> = emptyMap()
    ) : MyVisitsHubUiState
    data class Error(val message: String) : MyVisitsHubUiState
}
