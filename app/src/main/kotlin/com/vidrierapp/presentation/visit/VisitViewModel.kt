package com.vidrierapp.presentation.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.usecase.GetVisitsUseCase
import com.vidrierapp.domain.usecase.LogVisitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class VisitViewModel(
    private val logVisitUseCase: LogVisitUseCase,
    private val getVisitsUseCase: GetVisitsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitUiState())
    val uiState: StateFlow<VisitUiState> = _uiState.asStateFlow()

    fun initForShop(shopId: String) {
        _uiState.update { it.copy(shopId = shopId) }
        viewModelScope.launch {
            getVisitsUseCase(shopId).collect { visits ->
                _uiState.update { it.copy(pastVisits = visits) }
            }
        }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun incrementPedestrians() {
        _uiState.update { it.copy(pedestrianCount = it.pedestrianCount + 1) }
    }

    fun decrementPedestrians() {
        _uiState.update { it.copy(pedestrianCount = (it.pedestrianCount - 1).coerceAtLeast(0)) }
    }

    fun saveVisit() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val visit = Visit(
                id = UUID.randomUUID().toString(),
                shopId = current.shopId,
                timestamp = System.currentTimeMillis(),
                notes = current.notes,
                photoUris = current.photoUris,
                pedestrianCount = current.pedestrianCount
            )
            val result = logVisitUseCase(visit)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message) }
                }
            )
        }
    }
}
