package com.vidrierapp.data.repository

import com.vidrierapp.data.local.dao.CachedLocalDao
import com.vidrierapp.data.local.entity.CachedLocalEntity
import com.vidrierapp.data.mapper.toDomain
import com.vidrierapp.data.remote.api.VidrierAppApiService
import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.domain.model.CachedShop
import com.vidrierapp.domain.model.SHOP_CACHE_TTL_MILLIS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.repository.CachedShopRepository
import com.vidrierapp.domain.repository.ShopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class RetrofitShopRepository(
    private val apiService: VidrierAppApiService,
    private val cachedLocalDao: CachedLocalDao,
    private val json: Json,
    private val clock: () -> Long
) : ShopRepository, CachedShopRepository {

    override fun getShops(categoryFilter: String?): Flow<Result<List<Shop>>> = flow {
        try {
            val dtoList = apiService.getShops(categoryFilter)
            emit(Result.success(dtoList.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getShopDetail(shopId: String): Flow<Result<Shop>> = flow {
        val dto = try {
            apiService.getShopDetail(shopId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = readValid(shopId)
            emit(cached?.let { Result.success(it.shop) } ?: Result.failure(e))
            return@flow
        }
        runCatching { write(dto) }
        emit(Result.success(dto.toDomain()))
    }

    override fun getCachedShops(): Flow<List<CachedShop>> = flow {
        emitAll(cachedLocalDao.getCachedShops(cutoff()).map { entries -> entries.map { it.toCachedShop() } })
    }

    override suspend fun purgeExpiredCache() {
        cachedLocalDao.deleteExpired(cutoff())
    }

    private fun cutoff(): Long = clock() - SHOP_CACHE_TTL_MILLIS

    private suspend fun write(dto: ShopDto) {
        cachedLocalDao.upsert(
            CachedLocalEntity(
                shopId = dto.id,
                payloadJson = json.encodeToString(ShopDto.serializer(), dto),
                cachedAt = clock()
            )
        )
    }

    private suspend fun readValid(shopId: String): CachedShop? {
        purgeExpiredCache()
        return cachedLocalDao.getValid(shopId, cutoff())?.toCachedShop()
    }

    private fun CachedLocalEntity.toCachedShop(): CachedShop = CachedShop(
        shop = json.decodeFromString(ShopDto.serializer(), payloadJson).toDomain(),
        savedAt = cachedAt
    )
}
