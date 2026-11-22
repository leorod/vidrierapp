package com.vidrierapp.data.repository

import com.vidrierapp.data.mapper.toDomain
import com.vidrierapp.data.remote.api.VidrierAppApiService
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RetrofitShopRepository(
    private val apiService: VidrierAppApiService
) : ShopRepository {

    override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> = flow {
        try {
            val dtoList = apiService.getShops(categoryFilter)
            emit(Result.success(dtoList.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getShopDetail(shopId: String): Flow<Result<Shop>> = flow {
        try {
            val dto = apiService.getShopDetail(shopId)
            emit(Result.success(dto.toDomain()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
