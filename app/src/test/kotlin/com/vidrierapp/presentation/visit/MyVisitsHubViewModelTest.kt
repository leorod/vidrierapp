package com.vidrierapp.presentation.visit

import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository
import com.vidrierapp.domain.usecase.GetAllVisitsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyVisitsHubViewModelTest {

    private class FakeVisitRepository(
        private val allVisits: Flow<List<Visit>>
    ) : VisitRepository {
        override fun getVisitsForShop(shopId: String): Flow<List<Visit>> = flowOf(emptyList())
        override fun getAllVisits(): Flow<List<Visit>> = allVisits
        override suspend fun saveVisit(visit: Visit): Result<Unit> = Result.success(Unit)
    }

    private fun visit(id: String, timestamp: Long) = Visit(
        id = id,
        shopId = "shop-$id",
        timestamp = timestamp,
        notes = "nota $id",
        pedestrianCount = 3
    )

    private fun createViewModel(visits: Flow<List<Visit>>) =
        MyVisitsHubViewModel(GetAllVisitsUseCase(FakeVisitRepository(visits)))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits Success with visits in the given order`() {
        val visits = listOf(visit("2", 200L), visit("1", 100L))

        val viewModel = createViewModel(flowOf(visits))

        assertEquals(MyVisitsHubUiState.Success(visits), viewModel.uiState.value)
    }

    @Test
    fun `emits Success with empty list when there are no visits`() {
        val viewModel = createViewModel(flowOf(emptyList()))

        assertEquals(MyVisitsHubUiState.Success(emptyList()), viewModel.uiState.value)
    }

    @Test
    fun `emits Error when the flow throws`() {
        val failing = flow<List<Visit>> { throw IllegalStateException("boom") }

        val viewModel = createViewModel(failing)

        val state = viewModel.uiState.value
        assertTrue(state is MyVisitsHubUiState.Error)
        assertEquals("boom", (state as MyVisitsHubUiState.Error).message)
    }
}
