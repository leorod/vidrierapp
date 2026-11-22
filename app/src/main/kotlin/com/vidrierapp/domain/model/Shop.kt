package com.vidrierapp.domain.model

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
    val contactAgency: String = ""
)
