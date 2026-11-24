package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.CachedShop
import com.vidrierapp.domain.repository.CachedShopRepository
import kotlinx.coroutines.flow.Flow

class GetCachedShopsUseCase(
    private val cachedShopRepository: CachedShopRepository
) {
    operator fun invoke(): Flow<List<CachedShop>> {
        return cachedShopRepository.getCachedShops()
    }
}
