package com.vidrierapp.domain.model

data class Visit(
    val id: String,
    val shopId: String,
    val timestamp: Long,
    val notes: String,
    val photoUris: List<String> = emptyList(),
    val pedestrianCount: Int = 0
)
