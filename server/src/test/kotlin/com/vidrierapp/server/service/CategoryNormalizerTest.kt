package com.vidrierapp.server.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `classifies alias and fuzzy category input`() {
        val normalizer = CategoryNormalizer(AliasRepository())

        val alias = normalizer.normalize("Café", null)
        val fuzzy = normalizer.normalize(null, "panadria")

        assertEquals("cafeteria", alias.slug)
        assertTrue(alias.classified)
        assertEquals("panaderia", fuzzy.slug)
        assertTrue(fuzzy.classified)
    }

    @Test
    fun `marks unmatched free text as unclassified`() {
        val normalized = CategoryNormalizer(AliasRepository()).normalize(null, "reparacion de drones")

        assertEquals("other", normalized.slug)
        assertFalse(normalized.classified)
        assertEquals("reparacion de drones", normalized.rawInput)
    }
}

private class AliasRepository : VidrierAppRepository(UnsupportedDataSource()) {
    override fun categoryAliases() = mapOf("cafe" to "cafeteria", "panaderia" to "panaderia")
}

private class UnsupportedDataSource : javax.sql.DataSource {
    override fun getConnection() = throw UnsupportedOperationException()
    override fun getConnection(username: String?, password: String?) = throw UnsupportedOperationException()
    override fun <T : Any?> unwrap(iface: Class<T>?) = throw UnsupportedOperationException()
    override fun isWrapperFor(iface: Class<*>?) = false
    override fun getLogWriter() = null
    override fun setLogWriter(out: java.io.PrintWriter?) = Unit
    override fun setLoginTimeout(seconds: Int) = Unit
    override fun getLoginTimeout() = 0
    override fun getParentLogger() = java.util.logging.Logger.getGlobal()
}
