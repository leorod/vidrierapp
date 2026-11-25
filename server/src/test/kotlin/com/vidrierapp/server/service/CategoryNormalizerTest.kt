package com.vidrierapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryNormalizerTest {
    @Test
    fun `sanitizes accents punctuation and whitespace`() {
        val normalizer = CategoryNormalizer()

        assertEquals("cafe bar", normalizer.sanitize("  Café, Bar! "))
    }

    @Test
    fun `calculates levenshtein distance for common typing errors`() {
        val normalizer = CategoryNormalizer()

        assertEquals(1, normalizer.levenshtein("panadria", "panaderia"))
        assertEquals(1, normalizer.levenshtein("verduleris", "verduleria"))
    }
}
