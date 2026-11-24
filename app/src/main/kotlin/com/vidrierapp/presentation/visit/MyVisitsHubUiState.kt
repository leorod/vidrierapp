package com.vidrierapp.presentation.visit

import com.vidrierapp.domain.model.Visit

sealed interface MyVisitsHubUiState {
    data object Loading : MyVisitsHubUiState
    data class Success(val visits: List<Visit> = emptyList()) : MyVisitsHubUiState
    data class Error(val message: String) : MyVisitsHubUiState
}
