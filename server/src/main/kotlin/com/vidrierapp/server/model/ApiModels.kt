package com.vidrierapp.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val message: String)

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
    val firstVoteAt: String? = null,
    val lastVoteAt: String? = null,
    val isSampleSufficient: Boolean,
    val metricsAvailable: Boolean,
    val votes: List<CategoryMetricResponse> = emptyList(),
    val frequency: List<FrequencyMetricResponse> = emptyList(),
    val hourlyDistribution: List<HourlyMetricResponse> = emptyList(),
    val contactAgency: String = "",
)

@Serializable
data class CategoryMetricResponse(val categoryId: String, val categoryName: String, val voteCount: Int, val percentage: Float)

@Serializable
data class FrequencyMetricResponse(val frequency: String, val voteCount: Int)

@Serializable
data class HourlyMetricResponse(val dayOfWeek: Int, val hourBucket: Int, val voteCount: Int)

@Serializable
data class CreateVoteRequest(val shopId: String, val category: String? = null, val otherCategory: String? = null, val frequency: String? = null)

@Serializable
data class VoteResponse(val id: String, val category: String, val classified: Boolean)

@Serializable
data class TrialRequest(val deviceId: String)

@Serializable
data class TrialResponse(val deviceId: String, val status: String, val expiresAt: String, val metricsAvailable: Boolean)
