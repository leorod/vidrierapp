package com.vidrierapp.domain.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkMonitor(initialOnline: Boolean = true) : NetworkMonitor {
    val online = MutableStateFlow(initialOnline)
    override val isOnline: Flow<Boolean> = online
}
