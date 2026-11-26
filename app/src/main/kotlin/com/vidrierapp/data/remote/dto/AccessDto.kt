package com.vidrierapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data object TrialRequestDto

@Serializable
data class AccessDto(val status: String, val expiresAt: String)

@Serializable
data class CheckoutRequestDto(val paymentToken: String)
