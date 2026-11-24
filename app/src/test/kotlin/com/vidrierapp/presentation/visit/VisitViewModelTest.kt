package com.vidrierapp.presentation.visit

import androidx.lifecycle.SavedStateHandle
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository
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

    private fun createViewModel(
        repository: FakeVisitRepository = FakeVisitRepository(),
        handle: SavedStateHandle = SavedStateHandle()
    ) = VisitViewModel(
        savedStateHandle = handle,
        logVisitUseCase = LogVisitUseCase(repository),
        getVisitsUseCase = GetVisitsUseCase(repository)
    )

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
}
