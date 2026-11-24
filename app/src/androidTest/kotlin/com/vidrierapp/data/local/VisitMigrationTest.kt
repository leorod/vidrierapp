package com.vidrierapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.data.local.database.MIGRATION_1_2
import com.vidrierapp.data.local.database.MIGRATION_2_3
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        val visits = runBlocking { database.visitDao().getAllVisits().first() }
        database.close()

        assertEquals(2, visits.size)
        assertEquals("shop-1", visits[0].shopId)
        assertEquals(4, visits[0].pedestrianCount)
        assertNull(visits[1].shopId)
        assertNull(visits[1].manualAddress)
    }
}
