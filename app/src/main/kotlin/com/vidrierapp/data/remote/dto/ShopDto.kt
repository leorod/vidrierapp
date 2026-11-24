package com.vidrierapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopDto(
    val id: String,
    val address: String,
    val neighborhood: String,
    val latitude: Double,
    val longitude: Double,
    val surfaceAreaM2: Int,
    val rentalPrice: String,
    val topCategoryName: String? = null,
    val totalVotes: Int = 0,
    val votes: List<VoteDto> = emptyList(),
    val contactAgency: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val firstVoteAt: Long? = null,
    val lastVoteAt: Long? = null,
    val frequencyCounts: Map<String, Int> = emptyMap(),
    val votesByWeekday: Map<String, Int> = emptyMap(),
    val votesByTimeSlot: Map<String, Int> = emptyMap(),
    val status: String? = null
)
