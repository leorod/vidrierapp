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
import com.vidrierapp.server.model.VoteResponse
import com.vidrierapp.server.model.LoginRequest
import com.vidrierapp.server.auth.AuthService
import com.vidrierapp.server.auth.UserRole
import com.vidrierapp.server.application.VidrierAppUseCases
import com.vidrierapp.server.service.Bounds
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

fun Application.configureRouting(useCases: VidrierAppUseCases, authService: AuthService) {
    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        route("/api/v1") {
            post("/auth/login") {
                val request = call.receive<LoginRequest>()
                require(request.username.isNotBlank() && request.password.isNotBlank()) { "Username and password are required" }
                call.respond(authService.login(request.username, request.password) ?: throw SecurityException("Invalid username or password"))
            }

            post("/votes") {
                val request = call.receive<CreateVoteRequest>()
                val fingerprint = (call.request.headers["X-Fingerprint-Hash"] ?: request.fingerprintHash)?.takeIf { it.length in 32..128 }
                    ?: throw IllegalArgumentException("X-Fingerprint-Hash header is required")
                val browserToken = (call.request.headers["X-Browser-Token"] ?: request.clientToken)?.takeIf { it.length in 16..256 }
                    ?: throw IllegalArgumentException("X-Browser-Token header is required")
                val vote = useCases.submitVote(call.parseUuid(request.resolvedShopId(), "shopId"), request.category, request.resolvedOther(), request.frequency, fingerprint, browserToken, call.clientIp())
                call.respond(HttpStatusCode.Created, VoteResponse(vote.id.toString(), vote.category, vote.classified))
            }

            authenticate("auth-jwt") {
            get("/shops") {
                val category = call.request.queryParameters["category"]
                call.respond(useCases.shops(category, call.boundsOrNull()))
            }

            get("/shops/{id}") {
                val id = call.uuidParameter("id")
                call.respond(useCases.shopDetail(id, call.authenticatedUsername()))
            }

            post("/shops/{id}/report-rented") {
                call.respond(HttpStatusCode.Accepted, useCases.reportRented(call.uuidParameter("id")))
            }

            get("/auth/access") {
                call.respond(useCases.subscription(call.authenticatedUsername()))
            }

            post("/auth/trial") {
                call.respond(useCases.startTrial(call.authenticatedUsername()))
            }

            post("/payments/checkout") {
                val request = call.receive<MockCheckoutRequest>()
                call.respond(useCases.checkoutPass(call.authenticatedUsername(), request.paymentToken))
            }

            get("/zones/summary") { call.respond(useCases.zoneSummaries()) }

            route("/admin") {
                post("/geo/resolve") {
                    call.requireAdmin()
                    val request = call.receive<GeoResolveRequest>()
                    call.respond(useCases.resolveLocation(request.placeId, request.address))
                }
                post("/shops") {
                    call.requireAdmin()
                    val request = call.receive<CreateShopRequest>()
                    call.respond(HttpStatusCode.Created, useCases.createShop(request))
                }
                post("/shops/{id}/status") {
                    call.requireAdmin()
                    useCases.updateShopStatus(call.uuidParameter("id"), call.receive<UpdateShopStatusRequest>().status)
                    call.respond(HttpStatusCode.NoContent)
                }
                post("/maintenance/run") {
                    call.requireAdmin()
                    call.respond(useCases.runMaintenance())
                }
                post("/categories/aliases") {
                    call.requireAdmin()
                    val request = call.receive<AddCategoryAliasRequest>()
                    useCases.addCategoryAlias(request.alias, request.canonicalSlug)
                    call.respond(HttpStatusCode.NoContent)
                }
                get("/categories/unclassified") {
                    call.requireAdmin()
                    call.respond(useCases.unclassifiedCategories())
                }
                post("/categories/reclassify") {
                    call.requireAdmin()
                    val request = call.receive<ReclassifyVotesRequest>()
                    val updated = useCases.reclassifyVotes(request.alias, request.canonicalSlug)
                    call.respond(mapOf("reclassifiedVotes" to updated))
                }
            }
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.requireAdmin() {
    if (principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() != UserRole.ADMIN.name) {
        throw SecurityException("Admin authentication required")
    }
}

private fun io.ktor.server.application.ApplicationCall.authenticatedUsername(): String =
    principal<JWTPrincipal>()?.payload?.subject ?: throw SecurityException("Authentication required")

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
