package com.vidrierapp.server.routes

import com.vidrierapp.server.model.CreateVoteRequest
import com.vidrierapp.server.model.CreateShopRequest
import com.vidrierapp.server.model.ErrorResponse
import com.vidrierapp.server.model.TrialRequest
import com.vidrierapp.server.model.MockCheckoutRequest
import com.vidrierapp.server.model.UpdateShopStatusRequest
import com.vidrierapp.server.model.GeoResolveRequest
import com.vidrierapp.server.model.AddCategoryAliasRequest
import com.vidrierapp.server.model.ReclassifyVotesRequest
import com.vidrierapp.server.model.MaintenanceRunResponse
import com.vidrierapp.server.model.VoteResponse
import com.vidrierapp.server.service.Bounds
import com.vidrierapp.server.service.CategoryNormalizer
import com.vidrierapp.server.service.VidrierAppRepository
import com.vidrierapp.server.service.MockPaymentProcessor
import com.vidrierapp.server.service.GoogleGeocodingService
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

fun Application.configureRouting(repository: VidrierAppRepository, normalizer: CategoryNormalizer, paymentProcessor: MockPaymentProcessor, geocodingService: GoogleGeocodingService, adminToken: String) {
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
                val fingerprint = (call.request.headers["X-Fingerprint-Hash"] ?: request.fingerprintHash)?.takeIf { it.length in 32..128 }
                    ?: throw IllegalArgumentException("X-Fingerprint-Hash header is required")
                val browserToken = (call.request.headers["X-Browser-Token"] ?: request.clientToken)?.takeIf { it.length in 16..256 }
                    ?: throw IllegalArgumentException("X-Browser-Token header is required")
                val category = normalizer.normalize(request.category, request.resolvedOther())
                val voteId = repository.submitVote(call.parseUuid(request.resolvedShopId(), "shopId"), category, request.frequency, fingerprint, browserToken, call.clientIp())
                call.respond(HttpStatusCode.Created, VoteResponse(voteId.toString(), category.slug, category.classified))
            }

            post("/shops/{id}/report-rented") {
                call.respond(HttpStatusCode.Accepted, repository.reportRented(call.uuidParameter("id")))
            }

            post("/auth/trial") {
                val request = call.receive<TrialRequest>()
                require(request.deviceId.isNotBlank()) { "deviceId is required" }
                call.respond(repository.startTrial(request.deviceId))
            }

            post("/payments/checkout") {
                val request = call.receive<MockCheckoutRequest>()
                require(request.deviceId.isNotBlank()) { "deviceId is required" }
                call.respond(repository.checkoutPass(request.deviceId, paymentProcessor, request.paymentToken))
            }

            get("/zones/summary") { call.respond(repository.zoneSummaries()) }

            route("/admin") {
                post("/geo/resolve") {
                    call.requireAdmin(adminToken)
                    call.respond(geocodingService.resolve(call.receive<GeoResolveRequest>().placeId))
                }
                post("/shops") {
                    call.requireAdmin(adminToken)
                    val request = call.receive<CreateShopRequest>()
                    val validLocation = geocodingService.validate(request.geoResolutionId, request.latitude, request.longitude, request.confirmFarLocation)
                    require(validLocation) { "Selected location is more than 150 meters from the resolved address; set confirmFarLocation to override" }
                    call.respond(HttpStatusCode.Created, repository.createShop(request.copy(locationOverrideUsed = request.locationOverrideUsed || request.confirmFarLocation)))
                }
                post("/shops/{id}/status") {
                    call.requireAdmin(adminToken)
                    repository.updateShopStatus(call.uuidParameter("id"), call.receive<UpdateShopStatusRequest>().status)
                    call.respond(HttpStatusCode.NoContent)
                }
                post("/maintenance/run") {
                    call.requireAdmin(adminToken)
                    val expiredShops = repository.expireStaleShops()
                    val expiredAccess = repository.expireAccessStatuses()
                    repository.refreshAggregates()
                    call.respond(MaintenanceRunResponse(expiredShops, expiredAccess, true))
                }
                post("/categories/aliases") {
                    call.requireAdmin(adminToken)
                    val request = call.receive<AddCategoryAliasRequest>()
                    repository.addCategoryAlias(request.alias, request.canonicalSlug)
                    call.respond(HttpStatusCode.NoContent)
                }
                get("/categories/unclassified") {
                    call.requireAdmin(adminToken)
                    call.respond(repository.unclassifiedCategories())
                }
                post("/categories/reclassify") {
                    call.requireAdmin(adminToken)
                    val request = call.receive<ReclassifyVotesRequest>()
                    val updated = repository.reclassifyVotes(request.alias, request.canonicalSlug)
                    repository.refreshAggregates()
                    call.respond(mapOf("reclassifiedVotes" to updated))
                }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.requireAdmin(expectedToken: String) {
    if (expectedToken.isBlank() || request.headers["Authorization"] != "Bearer $expectedToken") {
        throw SecurityException("Admin authentication required")
    }
}

private fun io.ktor.server.application.ApplicationCall.clientIp(): String =
    request.headers["X-Forwarded-For"]?.substringBefore(',')?.trim() ?: request.local.remoteHost

private fun io.ktor.server.application.ApplicationCall.uuidParameter(name: String): UUID = parseUuid(parameters[name], name)

private fun io.ktor.server.application.ApplicationCall.parseUuid(value: String?, name: String): UUID =
    value?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: throw IllegalArgumentException("Invalid $name")

private fun io.ktor.server.application.ApplicationCall.boundsOrNull(): Bounds? {
    val parameters = request.queryParameters
    val values = listOf("lonMin", "latMin", "lonMax", "latMax").map { parameters[it]?.toDoubleOrNull() }
    if (values.all { it == null }) return null
    if (values.any { it == null }) throw IllegalArgumentException("Bounding box requires lonMin, latMin, lonMax and latMax")
    val (lonMin, latMin, lonMax, latMax) = values.filterNotNull()
    require(lonMin < lonMax && latMin < latMax) { "Bounding box coordinates are invalid" }
    return Bounds(lonMin, latMin, lonMax, latMax)
}
