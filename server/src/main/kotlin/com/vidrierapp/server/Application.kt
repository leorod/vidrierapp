package com.vidrierapp.server

import com.vidrierapp.server.config.DatabaseFactory
import com.vidrierapp.server.auth.AuthConfig
import com.vidrierapp.server.auth.AuthService
import com.vidrierapp.server.auth.UserRole
import com.vidrierapp.server.model.ErrorResponse
import com.vidrierapp.server.routes.configureRouting
import com.vidrierapp.server.service.CategoryNormalizer
import com.vidrierapp.server.service.DuplicateVoteException
import com.vidrierapp.server.service.RateLimitExceededException
import com.vidrierapp.server.service.MaintenanceWorker
import com.vidrierapp.server.service.MockPaymentProcessor
import com.vidrierapp.server.service.GoogleGeocodingService
import com.vidrierapp.server.service.VidrierAppRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.http.content.default
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import java.io.File
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8081, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    val repository = VidrierAppRepository(DatabaseFactory.createDataSource())
    MaintenanceWorker(repository).start()
    configureApp(
        repository,
        MockPaymentProcessor(),
        GoogleGeocodingService(System.getenv("MAPS_API_KEY") ?: ""),
        AuthService(AuthConfig(
            jwtSecret = requiredEnvironment("JWT_SECRET"),
            adminUsername = requiredEnvironment("ADMIN_USERNAME"),
            adminPassword = requiredEnvironment("ADMIN_PASSWORD"),
            userUsername = requiredEnvironment("USER_USERNAME"),
            userPassword = requiredEnvironment("USER_PASSWORD"),
        )),
        System.getenv("WEB_DIR") ?: "web",
    )
}

fun Application.configureApp(
    repository: VidrierAppRepository,
    paymentProcessor: MockPaymentProcessor,
    geocodingService: GoogleGeocodingService,
    authService: AuthService,
    webDir: String? = null,
) {
    val normalizer = CategoryNormalizer(repository)

    install(CallLogging)
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Device-Id")
        allowHeader("X-Fingerprint-Hash")
        allowHeader("X-Browser-Token")
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "VidrierApp"
            verifier(authService.verifier)
            validate { credential ->
                val role = credential.payload.getClaim("role").asString()
                if (credential.payload.subject.isNullOrBlank() || role !in UserRole.entries.map { it.name }) null else JWTPrincipal(credential.payload)
            }
        }
    }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request")) }
        exception<NoSuchElementException> { call, cause -> call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Resource not found")) }
        exception<DuplicateVoteException> { call, cause -> call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Duplicate vote")) }
        exception<RateLimitExceededException> { call, cause -> call.respond(HttpStatusCode.TooManyRequests, ErrorResponse(cause.message ?: "Too many requests")) }
        exception<SecurityException> { call, cause -> call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Unauthorized")) }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error on ${call.request.local.uri}", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }
    configureRouting(repository, normalizer, paymentProcessor, geocodingService, authService)
    webDir?.let { dir -> File(dir).takeIf { it.isDirectory }?.let { root -> routing { staticFiles("/", root) { default("index.html") } } } }
}

private fun requiredEnvironment(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: error("$name must be configured")
