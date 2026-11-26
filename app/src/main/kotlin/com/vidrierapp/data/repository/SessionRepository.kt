package com.vidrierapp.data.repository

import android.content.SharedPreferences
import com.vidrierapp.data.remote.api.VidrierAppApiService
import com.vidrierapp.data.remote.dto.AuthResponseDto
import com.vidrierapp.data.remote.dto.LoginRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(val token: String, val username: String, val role: String)

class SessionRepository(
    private val preferences: SharedPreferences,
    private val api: VidrierAppApiService,
    private val onSessionStarted: suspend () -> Unit = {},
    private val onSessionEnded: suspend () -> Unit = {},
) {
    private val _session = MutableStateFlow(readSession())
    val session = _session.asStateFlow()

    suspend fun login(username: String, password: String): UserSession {
        val response = api.login(LoginRequestDto(username, password))
        val session = response.toSession()
        preferences.edit()
            .putString(TOKEN_KEY, session.token)
            .putString(USERNAME_KEY, session.username)
            .putString(ROLE_KEY, session.role)
            .apply()
        _session.value = session
        onSessionStarted()
        return session
    }

    suspend fun logout() {
        preferences.edit().clear().apply()
        _session.value = null
        onSessionEnded()
    }

    fun token(): String? = _session.value?.token

    private fun readSession(): UserSession? {
        val token = preferences.getString(TOKEN_KEY, null) ?: return null
        val username = preferences.getString(USERNAME_KEY, null) ?: return null
        val role = preferences.getString(ROLE_KEY, null) ?: return null
        return UserSession(token, username, role)
    }

    private fun AuthResponseDto.toSession() = UserSession(token, username, role)

    private companion object {
        const val TOKEN_KEY = "token"
        const val USERNAME_KEY = "username"
        const val ROLE_KEY = "role"
    }
}
