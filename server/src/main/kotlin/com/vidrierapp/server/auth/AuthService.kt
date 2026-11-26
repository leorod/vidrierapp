package com.vidrierapp.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.vidrierapp.server.model.AuthResponse
import com.vidrierapp.server.application.port.UserAccountRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.Base64
import java.util.Date

enum class UserRole { ADMIN, USER }

data class AuthConfig(
    val jwtSecret: String,
    val issuer: String = "vidrierapp",
    val audience: String = "vidrierapp-clients",
    val tokenLifetime: Duration = Duration.ofHours(8),
)

class AuthService(
    private val config: AuthConfig,
    private val repository: UserAccountRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun login(username: String, password: String): AuthResponse? {
        val user = repository.findAuthUser(username) ?: return null
        if (!PasswordHasher.matches(password, user.passwordHash)) return null
        val expiresAt = clock.instant().plus(config.tokenLifetime)
        val token = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(user.username)
            .withClaim("role", user.role.name)
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)
        return AuthResponse(token, user.username, user.role.name, expiresAt.toString())
    }
}

data class AuthUser(val username: String, val passwordHash: String, val role: UserRole)

object PasswordHasher {
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256

    fun hash(password: String): String {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        return encode(password, salt, ITERATIONS)
    }

    fun matches(password: String, storedHash: String): Boolean = runCatching {
        val (iterations, encodedSalt, encodedHash) = storedHash.split(':')
        val expected = Base64.getDecoder().decode(encodedHash)
        val actual = derive(password, Base64.getDecoder().decode(encodedSalt), iterations.toInt())
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)

    private fun encode(password: String, salt: ByteArray, iterations: Int): String =
        "$iterations:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(derive(password, salt, iterations))}"

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray =
        javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS))
            .encoded
}
