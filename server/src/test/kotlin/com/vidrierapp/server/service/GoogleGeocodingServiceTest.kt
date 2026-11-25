package com.vidrierapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoogleGeocodingServiceTest {
    private val service = GoogleGeocodingService("unused-for-distance-tests")

    @Test
    fun `calculates short geographic distances in meters`() {
        val distance = service.distanceMeters(-34.6570, -58.5030, -34.6579, -58.5030)

        assertTrue(distance in 95.0..105.0)
    }

    @Test
    fun `same coordinate has zero distance`() {
        assertEquals(0.0, service.distanceMeters(-34.6570, -58.5030, -34.6570, -58.5030))
    }
}
