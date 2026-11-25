package com.vidrierapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.data.local.dao.VisitDao
import com.vidrierapp.data.local.database.VidrierAppDatabase
import com.vidrierapp.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VisitDaoTest {

    private lateinit var database: VidrierAppDatabase
    private lateinit var dao: VisitDao

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, VidrierAppDatabase::class.java).build()
        dao = database.visitDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun visit(
        id: String,
        timestamp: Long,
        shopId: String? = "shop-1",
        manualAddress: String? = null,
        photoPaths: String = "[]",
        notes: String = "",
        pedestrianCount: Int = 0
    ) = VisitEntity(
        id = id,
        shopId = shopId,
        timestamp = timestamp,
        notes = notes,
        photoPaths = photoPaths,
        pedestrianCount = pedestrianCount,
        manualAddress = manualAddress
    )

    @Test
    fun getAllVisitsIsEmptyWhenNothingWasInserted() = runBlocking {
        assertTrue(dao.getAllVisits().first().isEmpty())
    }

    @Test
    fun getVisitsForShopIsEmptyWhenShopHasNoVisits() = runBlocking {
        dao.insertVisit(visit("a", 1, shopId = "shop-1"))

        assertTrue(dao.getVisitsForShop("shop-2").first().isEmpty())
    }

    @Test
    fun getAllVisitsReturnsMixedShopAndManualAddressVisitsNewestFirst() = runBlocking {
        dao.insertVisit(visit("oldest", 100, shopId = "shop-1"))
        dao.insertVisit(visit("newest", 300, shopId = null, manualAddress = "Calle Falsa 123"))
        dao.insertVisit(visit("middle", 200, shopId = "shop-2"))

        val ids = dao.getAllVisits().first().map { it.id }

        assertEquals(listOf("newest", "middle", "oldest"), ids)
    }

    @Test
    fun getVisitsForShopReturnsOnlyThatShopNewestFirst() = runBlocking {
        dao.insertVisit(visit("a", 100, shopId = "shop-1"))
        dao.insertVisit(visit("b", 400, shopId = "shop-2"))
        dao.insertVisit(visit("c", 300, shopId = "shop-1"))
        dao.insertVisit(visit("d", 500, shopId = null, manualAddress = "Av. Siempre Viva 742"))

        val ids = dao.getVisitsForShop("shop-1").first().map { it.id }

        assertEquals(listOf("c", "a"), ids)
    }

    @Test
    fun manualAddressVisitIsNotReturnedForAnyShop() = runBlocking {
        dao.insertVisit(visit("manual", 100, shopId = null, manualAddress = "Calle 1"))

        assertTrue(dao.getVisitsForShop("").first().isEmpty())
    }

    @Test
    fun insertPersistsAllFieldsForShopVisit() = runBlocking {
        dao.insertVisit(
            visit(
                id = "a",
                timestamp = 42,
                shopId = "shop-1",
                photoPaths = "[\"/p/1.jpg\",\"/p/2.jpg\"]",
                notes = "mucho movimiento",
                pedestrianCount = 17
            )
        )

        val stored = dao.getAllVisits().first().single()

        assertEquals("shop-1", stored.shopId)
        assertNull(stored.manualAddress)
        assertEquals(42L, stored.timestamp)
        assertEquals("mucho movimiento", stored.notes)
        assertEquals(17, stored.pedestrianCount)
        assertEquals("[\"/p/1.jpg\",\"/p/2.jpg\"]", stored.photoPaths)
    }

    @Test
    fun insertPersistsManualAddressWithNullShopId() = runBlocking {
        dao.insertVisit(visit("a", 42, shopId = null, manualAddress = "Calle Falsa 123"))

        val stored = dao.getAllVisits().first().single()

        assertNull(stored.shopId)
        assertEquals("Calle Falsa 123", stored.manualAddress)
    }

    @Test
    fun insertingSameIdReplacesTheExistingVisit() = runBlocking {
        dao.insertVisit(visit("a", 100, notes = "first", pedestrianCount = 1))
        dao.insertVisit(visit("a", 200, notes = "second", pedestrianCount = 9))

        val stored = dao.getAllVisits().first().single()

        assertEquals("second", stored.notes)
        assertEquals(9, stored.pedestrianCount)
        assertEquals(200L, stored.timestamp)
    }

    @Test
    fun visitsWithEqualTimestampAreAllReturned() = runBlocking {
        dao.insertVisit(visit("a", 100))
        dao.insertVisit(visit("b", 100))

        assertEquals(setOf("a", "b"), dao.getAllVisits().first().map { it.id }.toSet())
    }
}
