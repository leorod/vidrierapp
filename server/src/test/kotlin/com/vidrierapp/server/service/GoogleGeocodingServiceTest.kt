package com.vidrierapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun `place id takes precedence over the address`() {
        val query = service.geocodeUri("ChIJ123", "Alberdi 6900").rawQuery

        assertTrue(query.startsWith("place_id=ChIJ123&"))
    }

    @Test
    fun `address is encoded and restricted to Argentina`() {
        val query = service.geocodeUri(null, " Av. Alberdi 6900, Mataderos ").rawQuery

        assertTrue(query.startsWith("address=Av.+Alberdi+6900%2C+Mataderos&components=country:AR"))
    }

    @Test
    fun `missing place id and address is rejected`() {
        assertFailsWith<IllegalArgumentException> { service.geocodeUri(" ", null) }
    }
}
