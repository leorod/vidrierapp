package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow

class GetAllVisitsUseCase(
    private val visitRepository: VisitRepository
) {
    operator fun invoke(): Flow<List<Visit>> {
        return visitRepository.getAllVisits()
    }
}
