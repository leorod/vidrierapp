package com.vidrierapp.presentation.common

import com.vidrierapp.domain.network.FakeNetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStatusViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isOffline is true from the start when the monitor begins offline`() = runTest(UnconfinedTestDispatcher()) {
        val vm = NetworkStatusViewModel(FakeNetworkMonitor(initialOnline = false))
        val job = vm.isOffline.onEach { }.launchIn(this)

        assertTrue(vm.isOffline.value)
        job.cancel()
    }

    @Test
    fun `isOffline mirrors the monitor`() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeNetworkMonitor(initialOnline = true)
        val vm = NetworkStatusViewModel(monitor)
        val job = vm.isOffline.onEach { }.launchIn(this)

        assertFalse(vm.isOffline.value)
        monitor.online.value = false
        assertTrue(vm.isOffline.value)
        monitor.online.value = true
        assertFalse(vm.isOffline.value)
        job.cancel()
    }
}
