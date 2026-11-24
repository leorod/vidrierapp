package com.vidrierapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.data.local.dao.CachedLocalDao
import com.vidrierapp.data.local.database.VidrierAppDatabase
import com.vidrierapp.data.local.entity.CachedLocalEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CachedLocalDaoTest {

    private lateinit var database: VidrierAppDatabase
    private lateinit var dao: CachedLocalDao

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, VidrierAppDatabase::class.java).build()
        dao = database.cachedLocalDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertReplacesEntryWithSameShopId() = runBlocking {
        dao.upsert(CachedLocalEntity("s1", "a", 100))
        dao.upsert(CachedLocalEntity("s1", "b", 200))

        val entry = dao.getValid("s1", 0)

        assertEquals("b", entry?.payloadJson)
        assertEquals(200L, entry?.cachedAt)
    }

    @Test
    fun getValidExcludesEntriesAtOrBeforeCutoff() = runBlocking {
        dao.upsert(CachedLocalEntity("s1", "a", 100))

        assertNull(dao.getValid("s1", 100))
        assertEquals("a", dao.getValid("s1", 99)?.payloadJson)
    }

    @Test
    fun getCachedShopsReturnsValidEntriesNewestFirst() = runBlocking {
        dao.upsert(CachedLocalEntity("old", "a", 50))
        dao.upsert(CachedLocalEntity("mid", "b", 150))
        dao.upsert(CachedLocalEntity("new", "c", 250))

        val ids = dao.getCachedShops(100).first().map { it.shopId }

        assertEquals(listOf("new", "mid"), ids)
    }

    @Test
    fun deleteExpiredRemovesEntriesAtOrBeforeCutoffAndReturnsCount() = runBlocking {
        dao.upsert(CachedLocalEntity("a", "x", 100))
        dao.upsert(CachedLocalEntity("b", "x", 101))
        dao.upsert(CachedLocalEntity("c", "x", 50))

        val deleted = dao.deleteExpired(100)

        assertEquals(2, deleted)
        assertEquals(listOf("b"), dao.getCachedShops(0).first().map { it.shopId })
    }
}
