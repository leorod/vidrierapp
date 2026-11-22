package com.vidrierapp.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShopDetailViewModel(
    private val getShopDetailUseCase: GetShopDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopDetailUiState>(ShopDetailUiState.Loading)
    val uiState: StateFlow<ShopDetailUiState> = _uiState.asStateFlow()

    fun loadShop(shopId: String) {
        viewModelScope.launch {
            _uiState.value = ShopDetailUiState.Loading
            getShopDetailUseCase(shopId).collect { result ->
                result.fold(
                    onSuccess = { shop ->
                        _uiState.value = ShopDetailUiState.Success(shop = shop)
                    },
                    onFailure = { error ->
                        _uiState.value = ShopDetailUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}
