package com.vidrierapp.presentation.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.usecase.GetAllVisitsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MyVisitsHubViewModel(
    private val getAllVisitsUseCase: GetAllVisitsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyVisitsHubUiState>(MyVisitsHubUiState.Loading)
    val uiState: StateFlow<MyVisitsHubUiState> = _uiState.asStateFlow()

    init {
        loadVisits()
    }

    fun loadVisits() {
        viewModelScope.launch {
            _uiState.value = MyVisitsHubUiState.Loading
            getAllVisitsUseCase()
                .catch { error ->
                    _uiState.value = MyVisitsHubUiState.Error(error.message.orEmpty())
                }
                .collect { visits ->
                    _uiState.value = MyVisitsHubUiState.Success(visits)
                }
        }
    }
}
