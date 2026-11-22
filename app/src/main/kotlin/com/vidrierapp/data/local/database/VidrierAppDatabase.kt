package com.vidrierapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vidrierapp.data.local.dao.VisitDao
import com.vidrierapp.data.local.entity.VisitEntity

@Database(entities = [VisitEntity::class], version = 1, exportSchema = false)
abstract class VidrierAppDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
}
