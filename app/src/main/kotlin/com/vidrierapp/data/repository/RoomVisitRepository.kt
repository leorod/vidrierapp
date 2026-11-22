package com.vidrierapp.data.repository

import com.vidrierapp.data.local.dao.VisitDao
import com.vidrierapp.data.mapper.toDomain
import com.vidrierapp.data.mapper.toEntity
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomVisitRepository(
    private val visitDao: VisitDao
) : VisitRepository {

    override fun getVisitsForShop(shopId: String): Flow<List<Visit>> {
        return visitDao.getVisitsForShop(shopId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveVisit(visit: Visit): Result<Unit> {
        return try {
            visitDao.insertVisit(visit.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
