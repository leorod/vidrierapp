package com.vidrierapp.domain.usecase

import com.vidrierapp.domain.repository.ReportedShopsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeReportedShopsRepository(initial: Set<String> = emptySet()) : ReportedShopsRepository {
    val state = MutableStateFlow(initial)

    override val reportedShopIds: Flow<Set<String>> = state

    override suspend fun markReported(shopId: String) {
        state.value = state.value + shopId
    }
}
