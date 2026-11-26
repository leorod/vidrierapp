package com.vidrierapp.data.remote.api

import com.vidrierapp.data.remote.dto.CheckoutRequestDto
import com.vidrierapp.data.remote.dto.TrialRequestDto
import com.vidrierapp.domain.model.PassState
import com.vidrierapp.domain.model.TRIAL_DURATION_MILLIS
import java.time.Instant

private const val APPROVED_PAYMENT_TOKEN = "mock_approve"

class RemoteAccessSync(
    private val api: VidrierAppApiService,
) {
    suspend fun fetchPassState(): PassState {
        val access = api.access()
        val expiresAt = Instant.parse(access.expiresAt).toEpochMilli()
        return when (access.status) {
            "PASS_ACTIVE" -> PassState(trialStartedAt = expiresAt - TRIAL_DURATION_MILLIS, passExpiresAt = expiresAt)
            "NONE" -> PassState()
            else -> PassState(trialStartedAt = expiresAt - TRIAL_DURATION_MILLIS)
        }
    }

    suspend fun startTrial() {
        api.startTrial(TrialRequestDto)
    }

    suspend fun checkout() {
        api.checkout(CheckoutRequestDto(APPROVED_PAYMENT_TOKEN))
    }
}
