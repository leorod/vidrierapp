package com.vidrierapp.data.repository

import android.content.SharedPreferences
import com.vidrierapp.domain.model.PassState
import com.vidrierapp.domain.repository.PassRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val KEY_TRIAL_STARTED_AT = "trial_started_at"
private const val KEY_PASS_EXPIRES_AT = "pass_expires_at"
private const val KEY_INSTALLATION_ID = "installation_id"

class SharedPreferencesPassRepository(
    private val preferences: SharedPreferences,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) : PassRepository {

    private val state = MutableStateFlow(read())

    override val passState: Flow<PassState> = state.asStateFlow()

    override suspend fun savePassState(state: PassState) {
        preferences.edit().apply {
            writeOptional(KEY_TRIAL_STARTED_AT, state.trialStartedAt)
            writeOptional(KEY_PASS_EXPIRES_AT, state.passExpiresAt)
        }.commit()
        this.state.value = state
    }

    override suspend fun installationId(): String {
        preferences.getString(KEY_INSTALLATION_ID, null)?.let { return it }
        val id = idGenerator()
        preferences.edit().putString(KEY_INSTALLATION_ID, id).commit()
        return id
    }

    private fun read() = PassState(
        trialStartedAt = preferences.optionalLong(KEY_TRIAL_STARTED_AT),
        passExpiresAt = preferences.optionalLong(KEY_PASS_EXPIRES_AT)
    )

    private fun SharedPreferences.optionalLong(key: String): Long? =
        if (contains(key)) getLong(key, 0L) else null

    private fun SharedPreferences.Editor.writeOptional(key: String, value: Long?) {
        if (value == null) remove(key) else putLong(key, value)
    }
}
