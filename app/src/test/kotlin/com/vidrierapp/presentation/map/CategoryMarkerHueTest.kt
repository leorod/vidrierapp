package com.vidrierapp.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryMarkerHueTest {

    @Test
    fun `hue is deterministic for the same category`() {
        assertEquals(categoryMarkerHue("cafe"), categoryMarkerHue("cafe"), 0f)
    }

    @Test
    fun `hue stays within 0 to 359`() {
        listOf("", "cafe", "verduleria", "panaderia", "kiosco", "zzzzzzzzzzzzzzzzzzzz").forEach {
            val hue = categoryMarkerHue(it)
            assertTrue(hue >= 0f && hue <= 359f)
        }
    }
}
