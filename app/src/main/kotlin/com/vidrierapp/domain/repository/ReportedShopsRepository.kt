package com.vidrierapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface ReportedShopsRepository {
    val reportedShopIds: Flow<Set<String>>
    suspend fun markReported(shopId: String)
}
