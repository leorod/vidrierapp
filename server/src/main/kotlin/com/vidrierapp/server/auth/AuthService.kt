package com.vidrierapp.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.vidrierapp.server.model.AuthResponse
import java.time.Clock
import java.time.Duration
import java.util.Date

enum class UserRole { ADMIN, USER }

data class AuthConfig(
    val jwtSecret: String,
    val adminUsername: String,
    val adminPassword: String,
    val userUsername: String,
    val userPassword: String,
    val issuer: String = "vidrierapp",
    val audience: String = "vidrierapp-clients",
    val tokenLifetime: Duration = Duration.ofHours(8),
)

class AuthService(
    private val config: AuthConfig,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun login(username: String, password: String): AuthResponse? {
        val role = when {
            username == config.adminUsername && password == config.adminPassword -> UserRole.ADMIN
            username == config.userUsername && password == config.userPassword -> UserRole.USER
            else -> return null
        }
        val expiresAt = clock.instant().plus(config.tokenLifetime)
        val token = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(username)
            .withClaim("role", role.name)
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)
        return AuthResponse(token, username, role.name, expiresAt.toString())
    }
}
