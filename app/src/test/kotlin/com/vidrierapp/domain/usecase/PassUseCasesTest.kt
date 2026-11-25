package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.model.PASS_DURATION_MILLIS
import com.vidrierapp.domain.model.PassAccess
import com.vidrierapp.domain.model.PassState
import com.vidrierapp.domain.model.TRIAL_DURATION_MILLIS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassUseCasesTest {

    @Test
    fun `start trial records the clock time`() = runTest {
        val repository = FakePassRepository()

        val started = StartTrialUseCase(repository) { 100L }()

        assertTrue(started)
        assertEquals(PassState(trialStartedAt = 100L), repository.state.value)
    }

    @Test
    fun `start trial is single use`() = runTest {
        val repository = FakePassRepository(PassState(trialStartedAt = 100L))

        val started = StartTrialUseCase(repository) { 999L }()

        assertFalse(started)
        assertEquals(PassState(trialStartedAt = 100L), repository.state.value)
    }

    @Test
    fun `purchase sets expiry thirty days from now`() = runTest {
        val repository = FakePassRepository()

        PurchasePassUseCase(repository) { 200L }()

        assertEquals(PassState(passExpiresAt = 200L + PASS_DURATION_MILLIS), repository.state.value)
    }

    @Test
    fun `renewing an active pass extends from the current expiry`() = runTest {
        val expiry = 10_000L
        val repository = FakePassRepository(PassState(passExpiresAt = expiry))

        PurchasePassUseCase(repository) { 5_000L }()

        assertEquals(expiry + PASS_DURATION_MILLIS, repository.state.value.passExpiresAt)
    }

    @Test
    fun `renewing an expired pass restarts from now`() = runTest {
        val repository = FakePassRepository(PassState(passExpiresAt = 1_000L))

        PurchasePassUseCase(repository) { 5_000L }()

        assertEquals(5_000L + PASS_DURATION_MILLIS, repository.state.value.passExpiresAt)
    }

    @Test
    fun `purchase keeps the trial record`() = runTest {
        val repository = FakePassRepository(PassState(trialStartedAt = 1L))

        PurchasePassUseCase(repository) { 2L }()

        assertEquals(1L, repository.state.value.trialStartedAt)
    }

    @Test
    fun `access is unlocked one millisecond before the trial ends`() = runTest {
        val repository = FakePassRepository(PassState(trialStartedAt = 0L))

        val access = ObservePassAccessUseCase(repository) { TRIAL_DURATION_MILLIS - 1 }().first()

        assertEquals(PassAccess(hasActivePass = true, trialAvailable = false), access)
    }

    @Test
    fun `trial lasts exactly three days`() {
        assertEquals(3L * 24 * 60 * 60 * 1000, TRIAL_DURATION_MILLIS)
    }

    @Test
    fun `purchased pass is active one millisecond before thirty days and locked at thirty days`() = runTest {
        val repository = FakePassRepository()
        PurchasePassUseCase(repository) { 500L }()

        assertEquals(30L * 24 * 60 * 60 * 1000, PASS_DURATION_MILLIS)
        assertTrue(repository.state.value.isPassActive(500L + PASS_DURATION_MILLIS - 1))
        assertFalse(repository.state.value.isPassActive(500L + PASS_DURATION_MILLIS))
    }

    @Test
    fun `access is locked on a fresh install and trial is available`() = runTest {
        val access = ObservePassAccessUseCase(FakePassRepository()) { 0L }().first()

        assertEquals(PassAccess(hasActivePass = false, trialAvailable = true), access)
    }

    @Test
    fun `access is unlocked during the trial and trial is no longer available`() = runTest {
        val repository = FakePassRepository(PassState(trialStartedAt = 0L))

        val access = ObservePassAccessUseCase(repository) { 1L }().first()

        assertEquals(PassAccess(hasActivePass = true, trialAvailable = false), access)
    }

    @Test
    fun `access is locked after trial expiry without a pass`() = runTest {
        val repository = FakePassRepository(PassState(trialStartedAt = 0L))

        val access = ObservePassAccessUseCase(repository) { TRIAL_DURATION_MILLIS }().first()

        assertEquals(PassAccess(hasActivePass = false, trialAvailable = false), access)
    }

    @Test
    fun `access is unlocked with an active pass after the trial expired`() = runTest {
        val repository = FakePassRepository(
            PassState(trialStartedAt = 0L, passExpiresAt = TRIAL_DURATION_MILLIS * 2)
        )

        val access = ObservePassAccessUseCase(repository) { TRIAL_DURATION_MILLIS }().first()

        assertTrue(access.hasActivePass)
    }

    @Test
    fun `access flips to locked when the pass expires while observing`() = runTest {
        val repository = FakePassRepository(PassState(passExpiresAt = 1_000L))
        val emissions = mutableListOf<PassAccess>()
        val job = launch {
            ObservePassAccessUseCase(repository) { testScheduler.currentTime }().toList(emissions)
        }

        runCurrent()
        assertEquals(listOf(PassAccess(hasActivePass = true, trialAvailable = true)), emissions)

        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(PassAccess(hasActivePass = false, trialAvailable = true), emissions.last())
        job.cancel()
    }

    @Test
    fun `access unlocks right after a purchase`() = runTest {
        val repository = FakePassRepository()
        val emissions = mutableListOf<PassAccess>()
        val job = launch {
            ObservePassAccessUseCase(repository) { testScheduler.currentTime }().toList(emissions)
        }
        runCurrent()

        PurchasePassUseCase(repository) { testScheduler.currentTime }()
        runCurrent()

        assertTrue(emissions.last().hasActivePass)
        job.cancel()
    }
}
