package com.vidrierapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vidrierapp.data.local.dao.CachedLocalDao
import com.vidrierapp.data.local.dao.VisitDao
import com.vidrierapp.data.local.entity.CachedLocalEntity
import com.vidrierapp.data.local.entity.VisitEntity

@Database(entities = [VisitEntity::class, CachedLocalEntity::class], version = 4, exportSchema = false)
abstract class VidrierAppDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
    abstract fun cachedLocalDao(): CachedLocalDao
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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS cached_locals (" +
                "shopId TEXT NOT NULL, payloadJson TEXT NOT NULL, cachedAt INTEGER NOT NULL, PRIMARY KEY(shopId))"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE visits_new (" +
                "id TEXT NOT NULL, shopId TEXT, timestamp INTEGER NOT NULL, " +
                "notes TEXT NOT NULL, photoPaths TEXT NOT NULL, " +
                "pedestrianCount INTEGER NOT NULL, manualAddress TEXT, PRIMARY KEY(id))"
        )
        db.execSQL(
            "INSERT INTO visits_new (id, shopId, timestamp, notes, photoPaths, pedestrianCount, manualAddress) " +
                "SELECT id, shopId, timestamp, notes, '[]', pedestrianCount, manualAddress FROM visits"
        )
        db.execSQL("DROP TABLE visits")
        db.execSQL("ALTER TABLE visits_new RENAME TO visits")
    }
}
