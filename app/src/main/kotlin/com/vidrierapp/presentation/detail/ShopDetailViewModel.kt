package com.vidrierapp.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import com.vidrierapp.domain.usecase.ObservePassAccessUseCase
import com.vidrierapp.domain.usecase.PurchasePassUseCase
import com.vidrierapp.domain.usecase.StartTrialUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShopDetailViewModel(
    private val getShopDetailUseCase: GetShopDetailUseCase,
    private val observePassAccessUseCase: ObservePassAccessUseCase,
    private val startTrialUseCase: StartTrialUseCase,
    private val purchasePassUseCase: PurchasePassUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopDetailUiState>(ShopDetailUiState.Loading)
    val uiState: StateFlow<ShopDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentShopId: String? = null

    fun retry() {
        currentShopId?.let { loadShop(it) }
    }

    fun startTrial() {
        viewModelScope.launch { startTrialUseCase() }
    }

    fun purchasePass() {
        viewModelScope.launch { purchasePassUseCase() }
    }

    fun loadShop(shopId: String) {
        currentShopId = shopId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ShopDetailUiState.Loading
            combine(getShopDetailUseCase(shopId), observePassAccessUseCase()) { result, access ->
                result to access
            }.collect { (result, access) ->
                result.fold(
                    onSuccess = { shop ->
                        _uiState.value = ShopDetailUiState.Success(
                            shop = shop,
                            hasActivePass = access.hasActivePass,
                            trialAvailable = access.trialAvailable
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = ShopDetailUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}
