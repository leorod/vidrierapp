package com.vidrierapp.data.repository

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.domain.model.PassState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SharedPreferencesPassRepositoryTest {

    private val preferences = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("pass_state_test", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        preferences.edit().clear().commit()
    }

    @Test
    fun startsEmpty() = runBlocking {
        assertEquals(PassState(), SharedPreferencesPassRepository(preferences).passState.first())
    }

    @Test
    fun savedStateSurvivesANewInstance() = runBlocking {
        val saved = PassState(trialStartedAt = 10L, passExpiresAt = 20L)
        SharedPreferencesPassRepository(preferences).savePassState(saved)

        assertEquals(saved, SharedPreferencesPassRepository(preferences).passState.first())
    }

    @Test
    fun installationIdIsGeneratedOnceAndPersisted() = runBlocking {
        var generated = 0
        val repository = SharedPreferencesPassRepository(preferences) { "id-${++generated}" }

        val first = repository.installationId()
        val again = SharedPreferencesPassRepository(preferences) { "other" }.installationId()

        assertEquals("id-1", first)
        assertEquals("id-1", again)
    }
}
