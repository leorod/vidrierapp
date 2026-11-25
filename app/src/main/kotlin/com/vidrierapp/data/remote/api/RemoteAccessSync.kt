package com.vidrierapp.data.remote.api

import com.vidrierapp.data.remote.dto.CheckoutRequestDto
import com.vidrierapp.data.remote.dto.TrialRequestDto

private const val APPROVED_PAYMENT_TOKEN = "mock_approve"

class RemoteAccessSync(
    private val api: VidrierAppApiService,
    private val deviceId: suspend () -> String
) {
    suspend fun startTrial() {
        api.startTrial(TrialRequestDto(deviceId()))
    }

    suspend fun checkout() {
        api.checkout(CheckoutRequestDto(deviceId(), APPROVED_PAYMENT_TOKEN))
    }
}
