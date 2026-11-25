package com.vidrierapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MockPaymentProcessorTest {
    private val processor = MockPaymentProcessor()

    @Test
    fun `approves the documented success token`() {
        assertTrue(processor.charge("mock_approve").approved)
    }

    @Test
    fun `declines the documented failure token`() {
        val decision = processor.charge("mock_decline")

        assertFalse(decision.approved)
        assertEquals("Payment declined by mock provider", decision.failureReason)
    }

    @Test
    fun `rejects unknown tokens`() {
        assertFailsWith<IllegalArgumentException> { processor.charge("card_4242") }
    }
}
