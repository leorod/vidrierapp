package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Visit
import com.vidrierapp.domain.repository.VisitRepository

class LogVisitUseCase(
    private val visitRepository: VisitRepository
) {
    suspend operator fun invoke(visit: Visit): Result<Unit> {
        return visitRepository.saveVisit(visit)
    }
}
