package com.vidrierapp.server.routes

import com.vidrierapp.server.model.CreateVoteRequest
import com.vidrierapp.server.model.ErrorResponse
import com.vidrierapp.server.model.TrialRequest
import com.vidrierapp.server.model.VoteResponse
import com.vidrierapp.server.service.Bounds
import com.vidrierapp.server.service.CategoryNormalizer
import com.vidrierapp.server.service.VidrierAppRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

fun Application.configureRouting(repository: VidrierAppRepository, normalizer: CategoryNormalizer) {
    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        route("/api/v1") {
            get("/shops") {
                val category = call.request.queryParameters["category"]
                call.respond(repository.shops(category, call.boundsOrNull()))
            }

            get("/shops/{id}") {
                val id = call.uuidParameter("id")
                call.respond(repository.shopDetail(id, repository.hasActiveAccess(call.request.headers["X-Device-Id"])))
            }

            post("/votes") {
                val request = call.receive<CreateVoteRequest>()
                val fingerprint = call.request.headers["X-Fingerprint-Hash"]?.takeIf { it.length in 32..128 }
                    ?: throw IllegalArgumentException("X-Fingerprint-Hash header is required")
                val category = normalizer.normalize(request.category, request.otherCategory)
                val voteId = repository.submitVote(UUID.fromString(request.shopId), category, request.frequency, fingerprint, call.request.headers["X-Browser-Token"])
                call.respond(HttpStatusCode.Created, VoteResponse(voteId.toString(), category.slug, category.classified))
            }

            post("/shops/{id}/report-rented") {
                repository.reportRented(call.uuidParameter("id"))
                call.respond(HttpStatusCode.Accepted, mapOf("status" to "reported"))
            }

            post("/auth/trial") {
                val request = call.receive<TrialRequest>()
                require(request.deviceId.isNotBlank()) { "deviceId is required" }
                call.respond(repository.startTrial(request.deviceId))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.uuidParameter(name: String): UUID =
    parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: throw IllegalArgumentException("Invalid $name")

private fun io.ktor.server.application.ApplicationCall.boundsOrNull(): Bounds? {
    val parameters = request.queryParameters
    val values = listOf("lonMin", "latMin", "lonMax", "latMax").map { parameters[it]?.toDoubleOrNull() }
    if (values.all { it == null }) return null
    if (values.any { it == null }) throw IllegalArgumentException("Bounding box requires lonMin, latMin, lonMax and latMax")
    val (lonMin, latMin, lonMax, latMax) = values.filterNotNull()
    require(lonMin < lonMax && latMin < latMax) { "Bounding box coordinates are invalid" }
    return Bounds(lonMin, latMin, lonMax, latMax)
}
