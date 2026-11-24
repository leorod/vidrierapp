package com.vidrierapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vidrierapp.data.local.dao.VisitDao
import com.vidrierapp.data.local.entity.VisitEntity

@Database(entities = [VisitEntity::class], version = 2, exportSchema = false)
abstract class VidrierAppDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE visits_new (" +
                "id TEXT NOT NULL, shopId TEXT, timestamp INTEGER NOT NULL, " +
                "notes TEXT NOT NULL, photoUrisCsv TEXT NOT NULL, " +
                "pedestrianCount INTEGER NOT NULL, manualAddress TEXT, PRIMARY KEY(id))"
        )
        db.execSQL(
            "INSERT INTO visits_new (id, shopId, timestamp, notes, photoUrisCsv, pedestrianCount, manualAddress) " +
                "SELECT id, NULLIF(shopId, ''), timestamp, notes, photoUrisCsv, pedestrianCount, NULL FROM visits"
        )
        db.execSQL("DROP TABLE visits")
        db.execSQL("ALTER TABLE visits_new RENAME TO visits")
    }
}
