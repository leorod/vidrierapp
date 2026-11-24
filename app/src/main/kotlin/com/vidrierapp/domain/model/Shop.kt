package com.vidrierapp.domain.model

const val MIN_VOTES_FOR_METRICS = 15

data class Shop(
    val id: String,
    val address: String,
    val neighborhood: String,
    val latitude: Double,
    val longitude: Double,
    val surfaceAreaM2: Int,
    val rentalPrice: String,
    val topCategory: Category?,
    val totalVotes: Int = 0,
    val voteBreakdown: List<Vote> = emptyList(),
    val contactAgency: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val firstVoteAt: Long? = null,
    val lastVoteAt: Long? = null,
    val frequencyCounts: Map<ConsumptionFrequency, Int> = emptyMap(),
    val votesByWeekday: Map<Weekday, Int> = emptyMap(),
    val votesByTimeSlot: Map<TimeSlot, Int> = emptyMap(),
    val status: String? = null
) {
    val isSampleSufficient: Boolean get() = totalVotes >= MIN_VOTES_FOR_METRICS
}
