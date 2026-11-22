package com.vidrierapp.presentation.visit

import com.vidrierapp.domain.model.Visit

data class VisitUiState(
    val shopId: String = "",
    val notes: String = "",
    val pedestrianCount: Int = 0,
    val photoUris: List<String> = emptyList(),
    val pastVisits: List<Visit> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
