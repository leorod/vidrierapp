package com.vidrierapp.domain.repository

import com.vidrierapp.domain.model.Shop
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    fun getShops(categoryFilter: String? = null): Flow<Result<List<Shop>>>
    fun getShopDetail(shopId: String): Flow<Result<Shop>>
    suspend fun reportRented(shopId: String): Result<Unit>
}
