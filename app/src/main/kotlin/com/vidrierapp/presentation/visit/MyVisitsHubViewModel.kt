package com.vidrierapp.presentation.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.model.CachedShop
import com.vidrierapp.domain.usecase.GetAllVisitsUseCase
import com.vidrierapp.domain.usecase.GetCachedShopsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

class MyVisitsHubViewModel(
    private val getAllVisitsUseCase: GetAllVisitsUseCase,
    private val getCachedShopsUseCase: GetCachedShopsUseCase,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyVisitsHubUiState>(MyVisitsHubUiState.Loading)
    val uiState: StateFlow<MyVisitsHubUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadVisits()
    }

    fun loadVisits() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (_uiState.value !is MyVisitsHubUiState.Success) {
                _uiState.value = MyVisitsHubUiState.Loading
            }
            combine(getAllVisitsUseCase(), getCachedShopsUseCase()) { visits, cached ->
                val now = clock()
                MyVisitsHubUiState.Success(
                    visits = visits.sortedByDescending { it.timestamp },
                    savedShops = cached.sortedByDescending { it.savedAt }.map { it.toItem(now) },
                    shopNames = cached.associate { it.shop.id to it.shop.address }
                )
            }
                .catch { error ->
                    _uiState.value = MyVisitsHubUiState.Error(error.message.orEmpty())
                }
                .collect { _uiState.value = it }
        }
    }

    private fun CachedShop.toItem(now: Long) = SavedShopItem(
        shopId = shop.id,
        address = shop.address,
        neighborhood = shop.neighborhood,
        daysAgo = ((now - savedAt) / MILLIS_PER_DAY).toInt().coerceAtLeast(0)
    )
}
