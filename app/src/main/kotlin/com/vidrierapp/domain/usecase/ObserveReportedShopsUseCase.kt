package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.repository.ReportedShopsRepository
import kotlinx.coroutines.flow.Flow

class ObserveReportedShopsUseCase(
    private val reportedShopsRepository: ReportedShopsRepository
) {
    operator fun invoke(): Flow<Set<String>> = reportedShopsRepository.reportedShopIds
}
