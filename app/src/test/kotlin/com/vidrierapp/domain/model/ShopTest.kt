package com.vidrierapp.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopTest {

    private fun shopWithVotes(votes: Int) = Shop(
        id = "shop",
        address = "address",
        neighborhood = "neighborhood",
        latitude = 0.0,
        longitude = 0.0,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = null,
        totalVotes = votes
    )

    @Test
    fun `sample is insufficient with 14 votes`() {
        assertFalse(shopWithVotes(14).isSampleSufficient)
    }

    @Test
    fun `sample is sufficient with 15 votes`() {
        assertTrue(shopWithVotes(15).isSampleSufficient)
    }

    @Test
    fun `sample is sufficient with 16 votes`() {
        assertTrue(shopWithVotes(16).isSampleSufficient)
    }
}
