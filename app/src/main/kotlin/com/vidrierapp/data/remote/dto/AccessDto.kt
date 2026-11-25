package com.vidrierapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrialRequestDto(val deviceId: String)

@Serializable
data class CheckoutRequestDto(val deviceId: String, val paymentToken: String)
