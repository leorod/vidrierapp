package com.vidrierapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class AuthResponseDto(val token: String, val username: String, val role: String, val expiresAt: String)
