package com.vidrierapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReportRentedResponseDto(
    val shopId: String,
    val reportCount: Int,
    val listingStatus: String = "ACTIVE"
)
