package com.vidrierapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vidrierapp.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun getVisitsForShop(shopId: String): Flow<List<VisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)
}
