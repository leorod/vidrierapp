package com.vidrierapp.server.service

import com.vidrierapp.server.model.GeoResolveResponse
import com.vidrierapp.server.application.port.GeocodingGateway
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ResolvedLocation(val latitude: Double, val longitude: Double, val expiresAt: Instant)

class GoogleGeocodingService(private val apiKey: String) : GeocodingGateway {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val resolutions = ConcurrentHashMap<String, ResolvedLocation>()

    override fun resolve(placeId: String?, address: String?): GeoResolveResponse {
        require(apiKey.isNotBlank()) { "MAPS_API_KEY is not configured" }
        val request = HttpRequest.newBuilder(geocodeUri(placeId, address))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() == 200) { "Google Geocoding request failed" }
        val payload = Json.parseToJsonElement(response.body()).jsonObject
        require(payload["status"]?.toString()?.trim('"') == "OK") { "Google could not resolve the selected place" }
        val result = payload["results"]?.jsonArray?.firstOrNull()?.jsonObject
        val location = result?.get("geometry")?.jsonObject?.get("location")?.jsonObject
            ?: throw IllegalArgumentException("Google Geocoding returned no location")
        val latitude = location["lat"]?.toString()?.toDoubleOrNull() ?: throw IllegalArgumentException("Google Geocoding returned an invalid latitude")
        val longitude = location["lng"]?.toString()?.toDoubleOrNull() ?: throw IllegalArgumentException("Google Geocoding returned an invalid longitude")
        val resolutionId = UUID.randomUUID().toString()
        resolutions[resolutionId] = ResolvedLocation(latitude, longitude, Instant.now().plusSeconds(15 * 60))
        val formattedAddress = result["formatted_address"]?.toString()?.trim('"').orEmpty()
        return GeoResolveResponse(resolutionId, latitude, longitude, formattedAddress = formattedAddress)
    }

    internal fun geocodeUri(placeId: String?, address: String?): URI {
        val query = when {
            !placeId.isNullOrBlank() -> "place_id=${encode(placeId.trim())}"
            !address.isNullOrBlank() -> "address=${encode(address.trim())}&components=country:AR&region=ar"
            else -> throw IllegalArgumentException("placeId or address is required")
        }
        return URI("https://maps.googleapis.com/maps/api/geocode/json?$query&language=es&key=$apiKey")
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)

    override fun validate(resolutionId: String?, latitude: Double, longitude: Double, confirmFarLocation: Boolean): Boolean {
        val resolved = resolutionId?.let(resolutions::remove) ?: return confirmFarLocation
        require(resolved.expiresAt.isAfter(Instant.now())) { "Location resolution expired; resolve the address again" }
        return confirmFarLocation || distanceMeters(resolved.latitude, resolved.longitude, latitude, longitude) <= 150
    }

    internal fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return earthRadiusMeters * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}
