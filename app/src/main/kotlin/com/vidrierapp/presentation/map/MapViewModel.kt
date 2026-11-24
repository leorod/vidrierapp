package com.vidrierapp.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.usecase.GetShopsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val getShopsUseCase: GetShopsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var currentFilter: String? = null

    init {
        loadShops()
    }

    fun retry() {
        loadShops(currentFilter)
    }

    fun loadShops(categoryFilter: String? = null) {
        currentFilter = categoryFilter
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            getShopsUseCase(categoryFilter).collect { result ->
                result.fold(
                    onSuccess = { shops ->
                        _uiState.value = MapUiState.Success(
                            shops = shops,
                            selectedCategoryFilter = categoryFilter
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = MapUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    fun selectShop(shop: Shop?) {
        val current = _uiState.value
        if (current is MapUiState.Success) {
            _uiState.value = current.copy(selectedShop = shop)
        }
    }
}
