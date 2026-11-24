package com.vidrierapp.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.data.local.database.MIGRATION_1_2
import com.vidrierapp.data.local.database.MIGRATION_2_3
import com.vidrierapp.data.local.database.MIGRATION_3_4
import com.vidrierapp.data.local.database.VidrierAppDatabase
import com.vidrierapp.data.local.entity.CachedLocalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class CachedLocalMigrationTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "cache-migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migratesVersion2ToCacheTableKeepingVisits() {
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(
                "CREATE TABLE visits (id TEXT NOT NULL, shopId TEXT, timestamp INTEGER NOT NULL, " +
                    "notes TEXT NOT NULL, photoUrisCsv TEXT NOT NULL, pedestrianCount INTEGER NOT NULL, " +
                    "manualAddress TEXT, PRIMARY KEY(id))"
            )
            db.execSQL("INSERT INTO visits VALUES ('a', 'shop-1', 2, 'n1', '', 4, NULL)")
            db.version = 2
        }

        val database = Room.databaseBuilder(context, VidrierAppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
        val visits = runBlocking { database.visitDao().getAllVisits().first() }
        val cached = runBlocking {
            database.cachedLocalDao().upsert(CachedLocalEntity("shop-1", "{}", 10))
            database.cachedLocalDao().getCachedShops(0).first()
        }
        database.close()

        assertEquals(1, visits.size)
        assertEquals(4, visits[0].pedestrianCount)
        assertEquals(listOf("shop-1"), cached.map { it.shopId })
    }
}
