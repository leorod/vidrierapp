package com.vidrierapp.server.service

import java.text.Normalizer

data class NormalizedCategory(val slug: String, val classified: Boolean, val rawInput: String?)

class CategoryNormalizer(private val repository: VidrierAppRepository? = null) {
    fun normalize(category: String?, otherCategory: String?): NormalizedCategory {
        val input = otherCategory?.takeIf { it.isNotBlank() } ?: category?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("category or otherCategory is required")
        val normalized = sanitize(input)
        val aliases = requireNotNull(repository) { "A repository is required to normalize categories" }.categoryAliases()
        aliases[normalized]?.let { return NormalizedCategory(it, true, otherCategory) }
        val match = aliases.minByOrNull { levenshtein(normalized, it.key) }
        if (match != null && levenshtein(normalized, match.key) <= fuzzyThreshold(normalized)) {
            return NormalizedCategory(match.value, true, otherCategory)
        }
        return NormalizedCategory("other", false, input)
    }

    internal fun sanitize(value: String): String = Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "")
        .replace("[^a-z0-9 ]".toRegex(), "")
        .replace("\\s+".toRegex(), " ")

    private fun fuzzyThreshold(value: String) = if (value.length <= 4) 1 else 2

    internal fun levenshtein(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (left[i] == right[j]) 0 else 1)
            previous = current
        }
        return previous[right.length]
    }
}
