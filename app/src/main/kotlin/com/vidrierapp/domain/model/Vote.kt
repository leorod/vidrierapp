package com.vidrierapp.domain.model

data class Vote(
    val category: Category,
    val count: Int,
    val percentage: Float
)
