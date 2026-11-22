package com.vidrierapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VoteDto(
    val categoryId: String,
    val categoryName: String,
    val voteCount: Int,
    val percentage: Float
)
