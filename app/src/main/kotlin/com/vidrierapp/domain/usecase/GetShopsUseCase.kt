package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.ShopStatus
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetShopsUseCase(
    private val shopRepository: ShopRepository
) {
    operator fun invoke(categoryFilter: String? = null): Flow<Result<List<Shop>>> {
        return shopRepository.getShops(categoryFilter).map { result ->
            result.map { shops -> shops.filter { it.listingStatus != ShopStatus.RENTED } }
        }
    }
}
