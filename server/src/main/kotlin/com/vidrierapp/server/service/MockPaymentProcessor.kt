package com.vidrierapp.server.service

import com.vidrierapp.server.application.port.PaymentDecision
import com.vidrierapp.server.application.port.PaymentGateway
import java.util.UUID

class MockPaymentProcessor : PaymentGateway {
    override fun charge(paymentToken: String): PaymentDecision = when (paymentToken) {
        "mock_approve" -> PaymentDecision(approved = true)
        "mock_decline" -> PaymentDecision(approved = false, failureReason = "Payment declined by mock provider")
        else -> throw IllegalArgumentException("paymentToken must be mock_approve or mock_decline")
    }

    override fun providerReference(): String = "mock_${UUID.randomUUID()}"
}
