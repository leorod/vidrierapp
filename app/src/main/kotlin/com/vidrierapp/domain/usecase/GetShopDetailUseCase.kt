package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow

class GetShopDetailUseCase(
    private val shopRepository: ShopRepository
) {
    operator fun invoke(shopId: String): Flow<Result<Shop>> {
        return shopRepository.getShopDetail(shopId)
    }
}
