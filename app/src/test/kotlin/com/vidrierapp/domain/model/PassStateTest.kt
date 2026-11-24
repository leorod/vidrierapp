package com.vidrierapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassStateTest {

    @Test
    fun `fresh state has no active pass and trial available`() {
        val state = PassState()

        assertFalse(state.hasActivePass(0L))
        assertFalse(state.trialUsed)
        assertNull(state.activeUntil(0L))
    }

    @Test
    fun `trial is active until exactly three days after start`() {
        val state = PassState(trialStartedAt = 1_000L)

        assertTrue(state.isTrialActive(1_000L))
        assertTrue(state.isTrialActive(1_000L + TRIAL_DURATION_MILLIS - 1))
        assertFalse(state.isTrialActive(1_000L + TRIAL_DURATION_MILLIS))
        assertTrue(state.trialUsed)
    }

    @Test
    fun `pass is active until exactly its expiry`() {
        val state = PassState(passExpiresAt = 5_000L)

        assertTrue(state.isPassActive(4_999L))
        assertFalse(state.isPassActive(5_000L))
        assertFalse(state.hasActivePass(5_000L))
    }

    @Test
    fun `expired trial keeps trial used`() {
        val state = PassState(trialStartedAt = 0L)

        assertFalse(state.hasActivePass(TRIAL_DURATION_MILLIS))
        assertTrue(state.trialUsed)
    }

    @Test
    fun `activeUntil returns the latest active expiry`() {
        val state = PassState(trialStartedAt = 0L, passExpiresAt = TRIAL_DURATION_MILLIS + 10)

        assertEquals(TRIAL_DURATION_MILLIS + 10, state.activeUntil(1L))
        assertEquals(TRIAL_DURATION_MILLIS + 10, state.activeUntil(TRIAL_DURATION_MILLIS))
        assertNull(state.activeUntil(TRIAL_DURATION_MILLIS + 10))
    }
}
