package com.vidrierapp.domain.repository

import com.vidrierapp.domain.model.Visit
import kotlinx.coroutines.flow.Flow

interface VisitRepository {
    fun getVisitsForShop(shopId: String): Flow<List<Visit>>
    fun getAllVisits(): Flow<List<Visit>>
    suspend fun saveVisit(visit: Visit): Result<Unit>
}
