package com.vidrierapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data object TrialRequestDto

@Serializable
data class CheckoutRequestDto(val paymentToken: String)
