package com.vidrierapp.server

import com.vidrierapp.server.config.DatabaseFactory
import com.vidrierapp.server.model.ErrorResponse
import com.vidrierapp.server.routes.configureRouting
import com.vidrierapp.server.service.CategoryNormalizer
import com.vidrierapp.server.service.VidrierAppRepository
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8081, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    val repository = VidrierAppRepository(DatabaseFactory.createDataSource())
    val normalizer = CategoryNormalizer(repository)

    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader("X-Device-Id")
        allowHeader("X-Fingerprint-Hash")
        allowHeader("X-Browser-Token")
    }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid request")) }
        exception<NoSuchElementException> { call, cause -> call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Resource not found")) }
    }
    configureRouting(repository, normalizer)
}
