package com.vidrierapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val message: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val username: String, val role: String, val expiresAt: String)

@Serializable
data class ShopSummaryResponse(
    val id: String,
    val address: String,
    val neighborhood: String,
    val latitude: Double,
    val longitude: Double,
    val surfaceAreaM2: Int = 0,
    val rentalPrice: String = "",
    val topCategoryName: String? = null,
    val totalVotes: Int = 0,
    val status: String,
    val listingStatus: String = "ACTIVE",
)

@Serializable
data class ShopDetailResponse(
    val id: String,
    val address: String,
    val neighborhood: String,
    val latitude: Double,
    val longitude: Double,
    val surfaceAreaM2: Int = 0,
    val rentalPrice: String = "",
    val topCategoryName: String? = null,
    val totalVotes: Int,
    val status: String,
    val firstVoteAt: Long? = null,
    val lastVoteAt: Long? = null,
    val isSampleSufficient: Boolean,
    val metricsAvailable: Boolean,
    val votes: List<CategoryMetricResponse> = emptyList(),
    val frequency: List<FrequencyMetricResponse> = emptyList(),
    val hourlyDistribution: List<HourlyMetricResponse> = emptyList(),
    val contactAgency: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val listingStatus: String = "ACTIVE",
    val frequencyCounts: Map<String, Int> = emptyMap(),
    val votesByWeekday: Map<String, Int> = emptyMap(),
    val votesByTimeSlot: Map<String, Int> = emptyMap(),
)

@Serializable
data class CategoryMetricResponse(val categoryId: String, val categoryName: String, val voteCount: Int, val percentage: Float)

@Serializable
data class FrequencyMetricResponse(val frequency: String, val voteCount: Int)

@Serializable
data class HourlyMetricResponse(val dayOfWeek: Int, val hourBucket: Int, val voteCount: Int)

@Serializable
data class CreateVoteRequest(
    val shopId: String? = null,
    val localId: String? = null,
    val category: String? = null,
    val otherCategory: String? = null,
    val otherText: String? = null,
    val frequency: String? = null,
    val clientToken: String? = null,
    val fingerprintHash: String? = null,
) {
    fun resolvedShopId(): String = shopId ?: localId ?: throw IllegalArgumentException("shopId is required")
    fun resolvedOther(): String? = otherCategory ?: otherText
}

@Serializable
data class ReportRentedResponse(val shopId: String, val reportCount: Int, val listingStatus: String)

@Serializable
data class MaintenanceRunResponse(val expiredShops: Int, val expiredAccess: Int, val aggregatesRefreshed: Boolean)

@Serializable
data class VoteResponse(val id: String, val category: String, val classified: Boolean)

@Serializable
data object TrialRequest

@Serializable
data class TrialResponse(val username: String, val status: String, val expiresAt: String, val metricsAvailable: Boolean)

@Serializable
data class MockCheckoutRequest(val paymentToken: String)

@Serializable
data class MockCheckoutResponse(
    val paymentId: String,
    val providerReference: String,
    val status: String,
    val failureReason: String? = null,
    val pass: TrialResponse? = null,
)

@Serializable
data class CreateShopRequest(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val neighborhood: String = "",
    val surfaceAreaM2: Int = 0,
    val rentalPrice: String = "",
    val agency: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val placeId: String? = null,
    val locationOverrideUsed: Boolean = false,
    val geoResolutionId: String? = null,
    val confirmFarLocation: Boolean = false,
)

@Serializable
data class GeoResolveRequest(val placeId: String? = null, val address: String? = null)

@Serializable
data class GeoResolveResponse(val resolutionId: String, val latitude: Double, val longitude: Double, val radiusMeters: Int = 150, val formattedAddress: String = "")

@Serializable
data class AddCategoryAliasRequest(val alias: String, val canonicalSlug: String)

@Serializable
data class ReclassifyVotesRequest(val alias: String, val canonicalSlug: String)

@Serializable
data class UnclassifiedCategoryResponse(val rawCategory: String, val voteCount: Int)

@Serializable
data class UpdateShopStatusRequest(val status: String)

@Serializable
data class ZoneSummaryResponse(val neighborhood: String, val totalVotes: Int, val shopsWithVotes: Int, val topCategoryName: String? = null)
