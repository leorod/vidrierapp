package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAllVisitsUseCaseTest {

    private class FakeVisitRepository(private val visits: List<Visit>) : VisitRepository {
        override fun getVisitsForShop(shopId: String): Flow<List<Visit>> {
            return flowOf(visits.filter { it.shopId == shopId })
        }

        override fun getAllVisits(): Flow<List<Visit>> {
            return flowOf(visits)
        }

        override suspend fun saveVisit(visit: Visit): Result<Unit> {
            return Result.success(Unit)
        }
    }

    @Test
    fun `invoke returns all visits unchanged`() = runTest {
        val visits = listOf(
            Visit(id = "v2", shopId = "2", timestamp = 2000L, notes = "b"),
            Visit(id = "v1", shopId = "1", timestamp = 1000L, notes = "a", photoUris = listOf("uri1"), pedestrianCount = 7)
        )
        val useCase = GetAllVisitsUseCase(FakeVisitRepository(visits))

        assertEquals(visits, useCase().first())
    }

    @Test
    fun `invoke returns empty list when there are no visits`() = runTest {
        val useCase = GetAllVisitsUseCase(FakeVisitRepository(emptyList()))

        assertTrue(useCase().first().isEmpty())
    }
}
