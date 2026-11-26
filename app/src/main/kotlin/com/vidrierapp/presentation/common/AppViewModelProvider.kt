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
import com.vidrierapp.presentation.auth.AuthViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { MapViewModel(container().getShopsUseCase, container().locationProvider, container().getCachedShopsUseCase, container().networkMonitor) }
        initializer { ShopDetailViewModel(
                getShopDetailUseCase = container().getShopDetailUseCase,
                observePassAccessUseCase = container().observePassAccessUseCase,
                startTrialUseCase = container().startTrialUseCase,
                purchasePassUseCase = container().purchasePassUseCase,
                reportRentedUseCase = container().reportRentedUseCase,
                observeReportedShopsUseCase = container().observeReportedShopsUseCase,
                networkMonitor = container().networkMonitor
            ) }
        initializer {
            VisitViewModel(
                savedStateHandle = createSavedStateHandle(),
                logVisitUseCase = container().logVisitUseCase,
                getVisitsUseCase = container().getVisitsUseCase,
                photoStorage = container().photoStorage
            )
        }
        initializer { NetworkStatusViewModel(container().networkMonitor) }
        initializer { MyVisitsHubViewModel(container().getAllVisitsUseCase, container().getCachedShopsUseCase, container().clock) }
        initializer { AuthViewModel(container().sessionRepository) }
    }
}

private fun CreationExtras.container(): AppContainer =
    ((this[APPLICATION_KEY] as? VidrierAppApplication) ?: VidrierAppApplication.instance).container
