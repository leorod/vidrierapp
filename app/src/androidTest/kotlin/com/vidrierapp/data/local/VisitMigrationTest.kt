package com.vidrierapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.data.local.database.MIGRATION_1_2
import com.vidrierapp.data.local.database.MIGRATION_2_3
import com.vidrierapp.data.local.database.MIGRATION_3_4
import com.vidrierapp.data.local.database.VidrierAppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisitMigrationTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migratesVersion1VisitsKeepingRowsAndNullingEmptyShopId() {
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(
                "CREATE TABLE visits (id TEXT NOT NULL, shopId TEXT NOT NULL, timestamp INTEGER NOT NULL, " +
                    "notes TEXT NOT NULL, photoUrisCsv TEXT NOT NULL, pedestrianCount INTEGER NOT NULL, " +
                    "PRIMARY KEY(id))"
            )
            db.execSQL("INSERT INTO visits VALUES ('a', 'shop-1', 2, 'n1', '', 4)")
            db.execSQL("INSERT INTO visits VALUES ('b', '', 1, 'n2', '', 0)")
            db.version = 1
        }

        val database = Room.databaseBuilder(context, VidrierAppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
        val visits = runBlocking { database.visitDao().getAllVisits().first() }
        database.close()

        assertEquals(2, visits.size)
        assertEquals("shop-1", visits[0].shopId)
        assertEquals(4, visits[0].pedestrianCount)
        assertNull(visits[1].shopId)
        assertNull(visits[1].manualAddress)
    }

    @Test
    fun migratesVersion3VisitsToPhotoPathsKeepingRows() {
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(
                "CREATE TABLE visits (id TEXT NOT NULL, shopId TEXT, timestamp INTEGER NOT NULL, " +
                    "notes TEXT NOT NULL, photoUrisCsv TEXT NOT NULL, pedestrianCount INTEGER NOT NULL, " +
                    "manualAddress TEXT, PRIMARY KEY(id))"
            )
            db.execSQL(
                "CREATE TABLE cached_locals (shopId TEXT NOT NULL, payloadJson TEXT NOT NULL, " +
                    "cachedAt INTEGER NOT NULL, PRIMARY KEY(shopId))"
            )
            db.execSQL("INSERT INTO visits VALUES ('a', 'shop-1', 2, 'n1', '', 4, NULL)")
            db.execSQL("INSERT INTO visits VALUES ('b', NULL, 1, 'n2', '', 0, 'Calle 1')")
            db.version = 3
        }

        val database = Room.databaseBuilder(context, VidrierAppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
        val dao = database.visitDao()
        val visits = runBlocking { dao.getAllVisits().first() }
        runBlocking {
            dao.insertVisit(
                com.vidrierapp.data.local.entity.VisitEntity(
                    id = "c", shopId = "s", timestamp = 3, notes = "", photoPaths = "[\"/p/1.jpg\"]"
                )
            )
        }
        val withPhoto = runBlocking { dao.getAllVisits().first() }.first { it.id == "c" }
        database.close()

        assertEquals(2, visits.size)
        assertEquals("shop-1", visits.first { it.id == "a" }.shopId)
        assertEquals(4, visits.first { it.id == "a" }.pedestrianCount)
        assertEquals("Calle 1", visits.first { it.id == "b" }.manualAddress)
        assertEquals("[]", visits.first { it.id == "a" }.photoPaths)
        assertEquals("[\"/p/1.jpg\"]", withPhoto.photoPaths)
    }
}
