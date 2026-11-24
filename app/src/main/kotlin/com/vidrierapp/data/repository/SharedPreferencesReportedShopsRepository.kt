package com.vidrierapp.data.repository

import android.content.SharedPreferences
import com.vidrierapp.domain.repository.ReportedShopsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val KEY_REPORTED_SHOP_IDS = "reported_shop_ids"

class SharedPreferencesReportedShopsRepository(
    private val preferences: SharedPreferences
) : ReportedShopsRepository {

    private val state = MutableStateFlow(read())

    override val reportedShopIds: Flow<Set<String>> = state.asStateFlow()

    override suspend fun markReported(shopId: String) {
        val updated = state.value + shopId
        preferences.edit().putStringSet(KEY_REPORTED_SHOP_IDS, updated).commit()
        state.value = updated
    }

    private fun read(): Set<String> =
        preferences.getStringSet(KEY_REPORTED_SHOP_IDS, emptySet())?.toSet() ?: emptySet()
}
