package com.vidrierapp.server.service

import java.util.UUID

data class MockPaymentDecision(val approved: Boolean, val failureReason: String? = null)

class MockPaymentProcessor {
    fun charge(paymentToken: String): MockPaymentDecision = when (paymentToken) {
        "mock_approve" -> MockPaymentDecision(approved = true)
        "mock_decline" -> MockPaymentDecision(approved = false, failureReason = "Payment declined by mock provider")
        else -> throw IllegalArgumentException("paymentToken must be mock_approve or mock_decline")
    }

    fun providerReference(): String = "mock_${UUID.randomUUID()}"
}
