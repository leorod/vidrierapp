package com.vidrierapp.presentation.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vidrierapp.VidrierAppApplication
import com.vidrierapp.di.AppContainer
import com.vidrierapp.presentation.detail.ShopDetailViewModel
import com.vidrierapp.presentation.map.MapViewModel
import com.vidrierapp.presentation.visit.MyVisitsHubViewModel
import com.vidrierapp.presentation.visit.VisitViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { MapViewModel(container().getShopsUseCase, container().locationProvider) }
        initializer { ShopDetailViewModel(container().getShopDetailUseCase) }
        initializer {
            VisitViewModel(
                savedStateHandle = createSavedStateHandle(),
                logVisitUseCase = container().logVisitUseCase,
                getVisitsUseCase = container().getVisitsUseCase
            )
        }
        initializer { MyVisitsHubViewModel(container().getAllVisitsUseCase) }
    }
}

private fun CreationExtras.container(): AppContainer =
    ((this[APPLICATION_KEY] as? VidrierAppApplication) ?: VidrierAppApplication.instance).container
