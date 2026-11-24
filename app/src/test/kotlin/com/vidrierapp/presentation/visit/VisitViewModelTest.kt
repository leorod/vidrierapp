package com.vidrierapp.presentation.visit

import androidx.lifecycle.SavedStateHandle
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository
import com.vidrierapp.domain.storage.PhotoSlot
import com.vidrierapp.domain.storage.PhotoStorage
import com.vidrierapp.domain.usecase.GetVisitsUseCase
import com.vidrierapp.domain.usecase.LogVisitUseCase
import com.vidrierapp.presentation.navigation.NO_SHOP_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisitViewModelTest {

    private class FakeVisitRepository(
        private val saveResult: Result<Unit> = Result.success(Unit),
        private val shopVisits: List<Visit> = emptyList()
    ) : VisitRepository {
        val saved = mutableListOf<Visit>()
        override fun getVisitsForShop(shopId: String): Flow<List<Visit>> = flowOf(shopVisits)
        override fun getAllVisits(): Flow<List<Visit>> = flowOf(emptyList())
        override suspend fun saveVisit(visit: Visit): Result<Unit> {
            if (saveResult.isSuccess) saved.add(visit)
            return saveResult
        }
    }

    private class FakePhotoStorage : PhotoStorage {
        val files = mutableSetOf<String>()
        val deleted = mutableListOf<String>()
        private var counter = 0
        var lastSlot: PhotoSlot? = null

        override fun createPhoto(): PhotoSlot {
            val slot = PhotoSlot("/photos/${counter++}.jpg", "content://fake/${counter}")
            lastSlot = slot
            return slot
        }

        override fun exists(path: String): Boolean = path in files

        override fun delete(path: String) {
            files.remove(path)
            deleted.add(path)
        }

        fun captureWritesFile() {
            files.add(lastSlot!!.path)
        }
    }

    private val storage = FakePhotoStorage()

    private fun createViewModel(
        repository: FakeVisitRepository = FakeVisitRepository(),
        handle: SavedStateHandle = SavedStateHandle(),
        photoStorage: FakePhotoStorage = storage
    ) = VisitViewModel(
        savedStateHandle = handle,
        logVisitUseCase = LogVisitUseCase(repository),
        getVisitsUseCase = GetVisitsUseCase(repository),
        photoStorage = photoStorage
    )

    private fun VisitViewModel.capturePhoto(): String {
        startPhotoCapture()
        storage.captureWritesFile()
        val path = storage.lastSlot!!.path
        onPhotoCaptureResult(true)
        return path
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `notes are updated in state and stored in the saved state handle`() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle = handle)

        viewModel.updateNotes("vereda angosta")

        assertEquals("vereda angosta", viewModel.uiState.value.notes)
        assertEquals("vereda angosta", handle.get<String>("draft_notes"))
    }

    @Test
    fun `draft is restored from the saved state handle after recreation`() {
        val handle = SavedStateHandle()
        createViewModel(handle = handle).apply {
            updateNotes("borrador")
            updateManualAddress("Av. Rivadavia 1000")
        }

        val restored = createViewModel(handle = handle)

        assertEquals("borrador", restored.uiState.value.notes)
        assertEquals("Av. Rivadavia 1000", restored.uiState.value.manualAddress)
    }

    @Test
    fun `no shop requires manual address`() {
        val viewModel = createViewModel()

        viewModel.initForShop(NO_SHOP_ID)

        assertTrue(viewModel.uiState.value.requiresManualAddress)
    }

    @Test
    fun `save without address and without shop shows error and does not persist`() {
        val repository = FakeVisitRepository()
        val viewModel = createViewModel(repository)
        viewModel.initForShop(NO_SHOP_ID)
        viewModel.updateNotes("nota")
        viewModel.updateManualAddress("   ")

        viewModel.saveVisit()

        assertTrue(viewModel.uiState.value.showAddressError)
        assertFalse(viewModel.uiState.value.saveSuccess)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `editing the address clears the address error`() {
        val viewModel = createViewModel()
        viewModel.initForShop(NO_SHOP_ID)
        viewModel.saveVisit()

        viewModel.updateManualAddress("Corrientes 123")

        assertFalse(viewModel.uiState.value.showAddressError)
    }

    @Test
    fun `save with manual address persists visit with null shopId`() {
        val repository = FakeVisitRepository()
        val viewModel = createViewModel(repository)
        viewModel.initForShop(NO_SHOP_ID)
        viewModel.updateManualAddress("  Corrientes 123 ")
        viewModel.updateNotes("buena esquina")

        viewModel.saveVisit()

        val saved = repository.saved.single()
        assertNull(saved.shopId)
        assertEquals("Corrientes 123", saved.manualAddress)
        assertEquals("buena esquina", saved.notes)
        assertTrue(viewModel.uiState.value.saveSuccess)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `save with known shop persists shopId and no manual address`() {
        val repository = FakeVisitRepository()
        val viewModel = createViewModel(repository)
        viewModel.initForShop("shop-1")
        viewModel.updateManualAddress("ignorada")

        viewModel.saveVisit()

        val saved = repository.saved.single()
        assertEquals("shop-1", saved.shopId)
        assertNull(saved.manualAddress)
        assertTrue(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun `increment adds one and stores the count in the saved state handle`() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle = handle)

        viewModel.incrementPedestrians()
        viewModel.incrementPedestrians()

        assertEquals(2, viewModel.uiState.value.pedestrianCount)
        assertEquals(2, handle.get<Int>("draft_pedestrian_count"))
    }

    @Test
    fun `decrement subtracts one`() {
        val viewModel = createViewModel()
        repeat(3) { viewModel.incrementPedestrians() }

        viewModel.decrementPedestrians()

        assertEquals(2, viewModel.uiState.value.pedestrianCount)
    }

    @Test
    fun `decrement never goes below zero`() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle = handle)

        viewModel.decrementPedestrians()
        viewModel.decrementPedestrians()

        assertEquals(0, viewModel.uiState.value.pedestrianCount)
        assertEquals(0, handle.get<Int>("draft_pedestrian_count"))
    }

    @Test
    fun `pedestrian count is restored from the saved state handle`() {
        val handle = SavedStateHandle()
        createViewModel(handle = handle).apply {
            incrementPedestrians()
            incrementPedestrians()
            incrementPedestrians()
        }

        val restored = createViewModel(handle = handle)

        assertEquals(3, restored.uiState.value.pedestrianCount)
    }

    @Test
    fun `save persists the pedestrian count`() {
        val repository = FakeVisitRepository()
        val viewModel = createViewModel(repository)
        viewModel.initForShop("shop-1")
        repeat(5) { viewModel.incrementPedestrians() }

        viewModel.saveVisit()

        assertEquals(5, repository.saved.single().pedestrianCount)
    }

    @Test
    fun `save success clears the pedestrian draft`() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle = handle)
        viewModel.initForShop("shop-1")
        viewModel.incrementPedestrians()

        viewModel.saveVisit()

        assertNull(handle.get<Int>("draft_pedestrian_count"))
        assertEquals(0, createViewModel(handle = handle).uiState.value.pedestrianCount)
    }

    @Test
    fun `save failure keeps the pedestrian draft`() {
        val handle = SavedStateHandle()
        val repository = FakeVisitRepository(saveResult = Result.failure(IllegalStateException("db")))
        val viewModel = createViewModel(repository, handle)
        viewModel.initForShop("shop-1")
        viewModel.incrementPedestrians()

        viewModel.saveVisit()

        assertEquals(1, viewModel.uiState.value.pedestrianCount)
        assertEquals(1, handle.get<Int>("draft_pedestrian_count"))
    }

    @Test
    fun `save success clears the draft`() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle = handle)
        viewModel.initForShop("shop-1")
        viewModel.updateNotes("algo")

        viewModel.saveVisit()

        assertNull(handle.get<String>("draft_notes"))
        assertEquals("", createViewModel(handle = handle).uiState.value.notes)
    }

    @Test
    fun `save failure keeps the draft and flags the error`() {
        val handle = SavedStateHandle()
        val repository = FakeVisitRepository(saveResult = Result.failure(IllegalStateException("db")))
        val viewModel = createViewModel(repository, handle)
        viewModel.initForShop("shop-1")
        viewModel.updateNotes("no perder")

        viewModel.saveVisit()

        val state = viewModel.uiState.value
        assertTrue(state.saveFailed)
        assertFalse(state.saveSuccess)
        assertFalse(state.isSaving)
        assertEquals("no perder", state.notes)
        assertEquals("no perder", handle.get<String>("draft_notes"))
    }

    @Test
    fun `past visits are loaded for a known shop`() {
        val past = listOf(Visit(id = "v1", shopId = "shop-1", timestamp = 1L, notes = "n"))
        val viewModel = createViewModel(FakeVisitRepository(shopVisits = past))

        viewModel.initForShop("shop-1")

        assertEquals(past, viewModel.uiState.value.pastVisits)
    }

    @Test
    fun `successful capture adds the photo to the state`() {
        val viewModel = createViewModel()

        val uri = viewModel.startPhotoCapture()
        assertEquals(storage.lastSlot!!.captureUri, uri)
        storage.captureWritesFile()
        viewModel.onPhotoCaptureResult(true)

        assertEquals(listOf(storage.lastSlot!!.path), viewModel.uiState.value.photoUris)
    }

    @Test
    fun `cancelled capture adds nothing and deletes the pending file`() {
        val viewModel = createViewModel()

        viewModel.startPhotoCapture()
        val path = storage.lastSlot!!.path
        viewModel.onPhotoCaptureResult(false)

        assertTrue(viewModel.uiState.value.photoUris.isEmpty())
        assertTrue(path in storage.deleted)
    }

    @Test
    fun `result without a pending capture is ignored`() {
        val viewModel = createViewModel()

        viewModel.onPhotoCaptureResult(true)

        assertTrue(viewModel.uiState.value.photoUris.isEmpty())
    }

    @Test
    fun `removing a photo drops it from the state and deletes the file`() {
        val viewModel = createViewModel()
        val first = viewModel.capturePhoto()
        val second = viewModel.capturePhoto()

        viewModel.removePhoto(first)

        assertEquals(listOf(second), viewModel.uiState.value.photoUris)
        assertTrue(first in storage.deleted)
        assertFalse(storage.exists(first))
    }

    @Test
    fun `photos survive recreation through the saved state handle`() {
        val handle = SavedStateHandle()
        val path = createViewModel(handle = handle).capturePhoto()

        val restored = createViewModel(handle = handle)

        assertEquals(listOf(path), restored.uiState.value.photoUris)
    }

    @Test
    fun `a capture finished after process death is attached to the restored draft`() {
        val handle = SavedStateHandle()
        createViewModel(handle = handle).startPhotoCapture()
        storage.captureWritesFile()
        val path = storage.lastSlot!!.path

        val restored = createViewModel(handle = handle)
        restored.onPhotoCaptureResult(true)

        assertEquals(listOf(path), restored.uiState.value.photoUris)
    }

    @Test
    fun `restore drops photos whose file no longer exists`() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle = handle)
        val gone = viewModel.capturePhoto()
        val kept = viewModel.capturePhoto()
        storage.files.remove(gone)

        val restored = createViewModel(handle = handle)

        assertEquals(listOf(kept), restored.uiState.value.photoUris)
    }

    @Test
    fun `save includes the photos and keeps the files`() {
        val repository = FakeVisitRepository()
        val handle = SavedStateHandle()
        val viewModel = createViewModel(repository, handle)
        viewModel.initForShop("shop-1")
        val path = viewModel.capturePhoto()

        viewModel.saveVisit()

        assertEquals(listOf(path), repository.saved.single().photoUris)
        assertTrue(storage.exists(path))
        assertTrue(createViewModel(handle = handle).uiState.value.photoUris.isEmpty())
    }

    @Test
    fun `save failure keeps the photos in the draft`() {
        val repository = FakeVisitRepository(saveResult = Result.failure(RuntimeException()))
        val handle = SavedStateHandle()
        val viewModel = createViewModel(repository, handle)
        viewModel.initForShop("shop-1")
        val path = viewModel.capturePhoto()

        viewModel.saveVisit()

        assertEquals(listOf(path), createViewModel(handle = handle).uiState.value.photoUris)
        assertTrue(storage.exists(path))
    }

    @Test
    fun `discarding the draft deletes photos and the pending capture`() {
        val handle = SavedStateHandle()
        val viewModel = createViewModel(handle = handle)
        val path = viewModel.capturePhoto()
        viewModel.startPhotoCapture()
        val pending = storage.lastSlot!!.path
        storage.captureWritesFile()

        viewModel.discardDraftPhotos()

        assertFalse(storage.exists(path))
        assertFalse(storage.exists(pending))
        assertTrue(viewModel.uiState.value.photoUris.isEmpty())
        assertTrue(createViewModel(handle = handle).uiState.value.photoUris.isEmpty())
    }
}
