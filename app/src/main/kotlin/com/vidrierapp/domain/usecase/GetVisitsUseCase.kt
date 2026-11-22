package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow

class GetVisitsUseCase(
    private val visitRepository: VisitRepository
) {
    operator fun invoke(shopId: String): Flow<List<Visit>> {
        return visitRepository.getVisitsForShop(shopId)
    }
}
