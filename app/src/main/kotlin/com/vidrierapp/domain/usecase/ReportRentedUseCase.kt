package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.repository.ReportedShopsRepository
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.first

class AlreadyReportedException(shopId: String) : IllegalStateException("Shop $shopId already reported")

class ReportRentedUseCase(
    private val shopRepository: ShopRepository,
    private val reportedShopsRepository: ReportedShopsRepository
) {
    suspend operator fun invoke(shopId: String): Result<Unit> {
        if (shopId in reportedShopsRepository.reportedShopIds.first()) {
            return Result.failure(AlreadyReportedException(shopId))
        }
        return shopRepository.reportRented(shopId).onSuccess {
            reportedShopsRepository.markReported(shopId)
        }
    }
}
