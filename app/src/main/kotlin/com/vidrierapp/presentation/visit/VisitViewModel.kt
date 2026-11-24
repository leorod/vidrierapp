package com.vidrierapp.presentation.visit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.usecase.GetVisitsUseCase
import com.vidrierapp.domain.storage.PhotoStorage
import com.vidrierapp.domain.usecase.LogVisitUseCase
import com.vidrierapp.presentation.navigation.NO_SHOP_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class VisitViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val logVisitUseCase: LogVisitUseCase,
    private val getVisitsUseCase: GetVisitsUseCase,
    private val photoStorage: PhotoStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VisitUiState(
            notes = savedStateHandle[KEY_NOTES] ?: "",
            manualAddress = savedStateHandle[KEY_ADDRESS] ?: "",
            pedestrianCount = (savedStateHandle.get<Int>(KEY_PEDESTRIANS) ?: 0).coerceAtLeast(0),
            photoUris = restorePhotos()
        )
    )
    val uiState: StateFlow<VisitUiState> = _uiState.asStateFlow()

    private var pastVisitsJob: Job? = null

    fun initForShop(shopId: String) {
        if (_uiState.value.shopId == shopId && pastVisitsJob != null) return
        _uiState.update { it.copy(shopId = shopId) }
        pastVisitsJob?.cancel()
        if (shopId == NO_SHOP_ID) {
            pastVisitsJob = Job()
            return
        }
        pastVisitsJob = viewModelScope.launch {
            getVisitsUseCase(shopId).collect { visits ->
                _uiState.update { it.copy(pastVisits = visits.sortedByDescending { v -> v.timestamp }) }
            }
        }
    }

    fun updateNotes(notes: String) {
        savedStateHandle[KEY_NOTES] = notes
        _uiState.update { it.copy(notes = notes) }
    }

    fun updateManualAddress(address: String) {
        savedStateHandle[KEY_ADDRESS] = address
        _uiState.update {
            it.copy(manualAddress = address, showAddressError = false, saveFailed = false)
        }
    }

    fun incrementPedestrians() {
        updatePedestrians { it + 1 }
    }

    fun decrementPedestrians() {
        updatePedestrians { (it - 1).coerceAtLeast(0) }
    }

    private fun updatePedestrians(transform: (Int) -> Int) {
        val count = transform(_uiState.value.pedestrianCount)
        savedStateHandle[KEY_PEDESTRIANS] = count
        _uiState.update { it.copy(pedestrianCount = count) }
    }

    private fun restorePhotos(): List<String> {
        val restored = (savedStateHandle.get<ArrayList<String>>(KEY_PHOTOS) ?: arrayListOf())
            .filter(photoStorage::exists)
        savedStateHandle[KEY_PHOTOS] = ArrayList(restored)
        return restored
    }

    fun startPhotoCapture(): String {
        savedStateHandle.get<String>(KEY_PENDING_PHOTO)?.let(photoStorage::delete)
        val slot = photoStorage.createPhoto()
        savedStateHandle[KEY_PENDING_PHOTO] = slot.path
        return slot.captureUri
    }

    fun onPhotoCaptureResult(success: Boolean) {
        val pending = savedStateHandle.get<String>(KEY_PENDING_PHOTO) ?: return
        savedStateHandle.remove<String>(KEY_PENDING_PHOTO)
        if (!success || !photoStorage.exists(pending)) {
            photoStorage.delete(pending)
            return
        }
        updatePhotos { it + pending }
    }

    fun removePhoto(path: String) {
        if (path !in _uiState.value.photoUris) return
        photoStorage.delete(path)
        updatePhotos { list -> list.filterNot { it == path } }
    }

    fun discardDraftPhotos() {
        _uiState.value.photoUris.forEach(photoStorage::delete)
        savedStateHandle.get<String>(KEY_PENDING_PHOTO)?.let(photoStorage::delete)
        savedStateHandle.remove<String>(KEY_PENDING_PHOTO)
        savedStateHandle.remove<ArrayList<String>>(KEY_PHOTOS)
        _uiState.update { it.copy(photoUris = emptyList()) }
    }

    override fun onCleared() {
        if (!_uiState.value.saveSuccess) discardDraftPhotos()
    }

    private fun updatePhotos(transform: (List<String>) -> List<String>) {
        val photos = transform(_uiState.value.photoUris)
        savedStateHandle[KEY_PHOTOS] = ArrayList(photos)
        _uiState.update { it.copy(photoUris = photos) }
    }

    fun saveVisit() {
        val current = _uiState.value
        if (current.isSaving) return
        val address = current.manualAddress.trim()
        if (current.requiresManualAddress && address.isEmpty()) {
            _uiState.update { it.copy(showAddressError = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveFailed = false) }
            val visit = Visit(
                id = UUID.randomUUID().toString(),
                shopId = if (current.requiresManualAddress) null else current.shopId,
                manualAddress = if (current.requiresManualAddress) address else null,
                timestamp = System.currentTimeMillis(),
                notes = current.notes.trim(),
                photoUris = current.photoUris,
                pedestrianCount = current.pedestrianCount
            )
            logVisitUseCase(visit).fold(
                onSuccess = {
                    savedStateHandle.remove<String>(KEY_NOTES)
                    savedStateHandle.remove<String>(KEY_ADDRESS)
                    savedStateHandle.remove<Int>(KEY_PEDESTRIANS)
                    savedStateHandle.remove<ArrayList<String>>(KEY_PHOTOS)
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                },
                onFailure = {
                    _uiState.update { it.copy(isSaving = false, saveFailed = true) }
                }
            )
        }
    }

    private companion object {
        const val KEY_NOTES = "draft_notes"
        const val KEY_ADDRESS = "draft_manual_address"
        const val KEY_PEDESTRIANS = "draft_pedestrian_count"
        const val KEY_PHOTOS = "draft_photo_paths"
        const val KEY_PENDING_PHOTO = "draft_pending_photo"
    }
}
