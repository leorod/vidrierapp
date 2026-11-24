package com.vidrierapp.data.repository

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SharedPreferencesReportedShopsRepositoryTest {

    private val preferences = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("reported_shops_test", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        preferences.edit().clear().commit()
    }

    @Test
    fun startsEmpty() = runBlocking {
        assertEquals(emptySet<String>(), SharedPreferencesReportedShopsRepository(preferences).reportedShopIds.first())
    }

    @Test
    fun reportedIdsSurviveANewInstanceAndAccumulate() = runBlocking {
        val first = SharedPreferencesReportedShopsRepository(preferences)
        first.markReported("a")
        first.markReported("b")
        first.markReported("a")

        assertEquals(
            setOf("a", "b"),
            SharedPreferencesReportedShopsRepository(preferences).reportedShopIds.first()
        )
    }
}
