package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.CachedShop
import com.vidrierapp.domain.repository.CachedShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeCachedShopRepository(private val cached: List<CachedShop> = emptyList()) : CachedShopRepository {
    override fun getCachedShops(): Flow<List<CachedShop>> = flowOf(cached)
    override suspend fun purgeExpiredCache() = Unit
}
