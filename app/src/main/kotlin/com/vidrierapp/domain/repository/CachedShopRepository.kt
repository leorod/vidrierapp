package com.vidrierapp.domain.repository

import com.vidrierapp.domain.model.CachedShop
import kotlinx.coroutines.flow.Flow

interface CachedShopRepository {
    fun getCachedShops(): Flow<List<CachedShop>>
    suspend fun purgeExpiredCache()
}
