package com.vidrierapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidrierapp.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(val isLoading: Boolean = false, val error: String? = null)

class AuthViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    val session = sessionRepository.session
    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState = _loginState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState(error = "Ingresá usuario y contraseña.")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginUiState(isLoading = true)
            runCatching { sessionRepository.login(username.trim(), password) }
                .onSuccess { _loginState.value = LoginUiState() }
                .onFailure { _loginState.value = LoginUiState(error = "No se pudo iniciar sesión. Verificá tus credenciales.") }
        }
    }

    fun logout() = sessionRepository.logout()
}
