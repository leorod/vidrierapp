package com.vidrierapp.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.network.NetworkMonitor
import com.vidrierapp.domain.usecase.GetShopDetailUseCase
import com.vidrierapp.domain.usecase.ObservePassAccessUseCase
import com.vidrierapp.domain.usecase.PurchasePassUseCase
import com.vidrierapp.domain.usecase.StartTrialUseCase
import com.vidrierapp.domain.usecase.AlreadyReportedException
import com.vidrierapp.domain.usecase.ObserveReportedShopsUseCase
import com.vidrierapp.domain.usecase.ReportRentedUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShopDetailViewModel(
    private val getShopDetailUseCase: GetShopDetailUseCase,
    private val observePassAccessUseCase: ObservePassAccessUseCase,
    private val startTrialUseCase: StartTrialUseCase,
    private val purchasePassUseCase: PurchasePassUseCase,
    private val reportRentedUseCase: ReportRentedUseCase,
    observeReportedShopsUseCase: ObserveReportedShopsUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopDetailUiState>(ShopDetailUiState.Loading)
    val uiState: StateFlow<ShopDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentShopId: String? = null

    private val shopIdFlow = MutableStateFlow<String?>(null)
    private val dialogVisible = MutableStateFlow(false)
    private val submitting = MutableStateFlow(false)

    val reportState: StateFlow<ReportRentedUiState> = combine(
        shopIdFlow,
        dialogVisible,
        submitting,
        observeReportedShopsUseCase()
    ) { id, visible, isSubmitting, reported ->
        ReportRentedUiState(
            isDialogVisible = visible,
            isSubmitting = isSubmitting,
            alreadyReported = id != null && id in reported
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ReportRentedUiState())

    private val feedbackChannel = Channel<ReportRentedFeedback>(Channel.BUFFERED)
    val feedback: Flow<ReportRentedFeedback> = feedbackChannel.receiveAsFlow()

    fun onReportRentedClick() {
        if (reportState.value.alreadyReported) {
            feedbackChannel.trySend(ReportRentedFeedback.AlreadyReported)
        } else {
            dialogVisible.value = true
        }
    }

    fun dismissReportDialog() {
        dialogVisible.value = false
    }

    fun confirmReportRented() {
        val id = currentShopId ?: return
        if (submitting.value) return
        dialogVisible.value = false
        submitting.value = true
        viewModelScope.launch {
            val result = reportRentedUseCase(id)
            submitting.value = false
            val event = when {
                result.isSuccess -> ReportRentedFeedback.Success
                result.exceptionOrNull() is AlreadyReportedException -> ReportRentedFeedback.AlreadyReported
                else -> ReportRentedFeedback.Failure
            }
            feedbackChannel.send(event)
        }
    }

    fun retry() {
        currentShopId?.let { loadShop(it) }
    }

    fun startTrial() {
        viewModelScope.launch { startTrialUseCase() }
    }

    fun purchasePass() {
        viewModelScope.launch { purchasePassUseCase() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadShop(shopId: String) {
        currentShopId = shopId
        shopIdFlow.value = shopId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ShopDetailUiState.Loading
            observePassAccessUseCase()
                .distinctUntilChanged()
                .flatMapLatest { access -> getShopDetailUseCase(shopId).map { result -> result to access } }
                .collect { (result, access) ->
                result.fold(
                    onSuccess = { shop ->
                        _uiState.value = ShopDetailUiState.Success(
                            shop = shop,
                            hasActivePass = access.hasActivePass,
                            trialAvailable = access.trialAvailable
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = ShopDetailUiState.Error(
                            message = error.message.orEmpty(),
                            isOffline = !networkMonitor.isOnline.first()
                        )
                    }
                )
            }
        }
    }
}
