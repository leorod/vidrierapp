package com.vidrierapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vidrierapp.data.local.entity.CachedLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedLocalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CachedLocalEntity)

    @Query("SELECT * FROM cached_locals WHERE shopId = :shopId AND cachedAt > :cutoff LIMIT 1")
    suspend fun getValid(shopId: String, cutoff: Long): CachedLocalEntity?

    @Query("SELECT * FROM cached_locals WHERE cachedAt > :cutoff ORDER BY cachedAt DESC")
    fun getCachedShops(cutoff: Long): Flow<List<CachedLocalEntity>>

    @Query("DELETE FROM cached_locals WHERE cachedAt <= :cutoff")
    suspend fun deleteExpired(cutoff: Long): Int
}
