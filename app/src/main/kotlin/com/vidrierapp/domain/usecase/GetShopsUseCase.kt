package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow

class GetShopsUseCase(
    private val shopRepository: ShopRepository
) {
    operator fun invoke(categoryFilter: String? = null): Flow<Result<List<Shop>>> {
        return shopRepository.getShops(categoryFilter)
    }
}
