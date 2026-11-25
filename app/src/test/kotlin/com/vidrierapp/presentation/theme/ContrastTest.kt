package com.vidrierapp.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ContrastTest {

    private data class ContrastPair(val name: String, val foreground: Color, val background: Color, val minimum: Double)

    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun textPairs(scheme: ColorScheme, dimmed: DimmedColors): List<ContrastPair> {
        val text = 4.5
        val graphic = 3.0
        return listOf(
            ContrastPair("onSurface/surface", scheme.onSurface, scheme.surface, text),
            ContrastPair("onBackground/background", scheme.onBackground, scheme.background, text),
            ContrastPair("onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface, text),
            ContrastPair("onSurfaceVariant/surfaceContainerLow", scheme.onSurfaceVariant, scheme.surfaceContainerLow, text),
            ContrastPair("onSurface/surfaceContainer", scheme.onSurface, scheme.surfaceContainer, text),
            ContrastPair("onSurface/surfaceContainerHighest", scheme.onSurface, scheme.surfaceContainerHighest, text),
            ContrastPair("onPrimary/primary", scheme.onPrimary, scheme.primary, text),
            ContrastPair("onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer, text),
            ContrastPair("onSecondary/secondary", scheme.onSecondary, scheme.secondary, text),
            ContrastPair("onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer, text),
            ContrastPair("onTertiary/tertiary", scheme.onTertiary, scheme.tertiary, text),
            ContrastPair("onTertiaryContainer/tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer, text),
            ContrastPair("onError/error", scheme.onError, scheme.error, text),
            ContrastPair("onErrorContainer/errorContainer", scheme.onErrorContainer, scheme.errorContainer, text),
            ContrastPair("error/surface", scheme.error, scheme.surface, text),
            ContrastPair("primary/surface", scheme.primary, scheme.surface, text),
            ContrastPair("inverseOnSurface/inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface, text),
            ContrastPair("dimmed.content/dimmed.container", dimmed.content, dimmed.container, text),
            ContrastPair("dimmed.content/surface", dimmed.content, scheme.surface, text),
            ContrastPair("dimmed.content/surfaceContainerLow", dimmed.content, scheme.surfaceContainerLow, text),
            ContrastPair("outline/surface", scheme.outline, scheme.surface, graphic),
            ContrastPair("dimmed.outline/dimmed.container", dimmed.outline, dimmed.container, graphic),
            ContrastPair("dimmed.outline/surface", dimmed.outline, scheme.surface, graphic)
        )
    }

    private fun assertAll(label: String, pairs: List<ContrastPair>) {
        val failures = pairs.filter { ratio(it.foreground, it.background) < it.minimum }
            .map { "$label ${it.name} = ${"%.2f".format(ratio(it.foreground, it.background))} < ${it.minimum}" }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun lightSchemeMeetsWcagContrast() {
        assertAll("light", textPairs(LightColorScheme, LightDimmedColors))
    }

    @Test
    fun darkSchemeMeetsWcagContrast() {
        assertAll("dark", textPairs(DarkColorScheme, DarkDimmedColors))
    }

    @Test
    fun ratioMatchesWcagReferenceValues() {
        assertTrue(kotlin.math.abs(ratio(Color.Black, Color.White) - 21.0) < 0.01)
        assertTrue(kotlin.math.abs(ratio(Color.White, Color.White) - 1.0) < 0.01)
        assertTrue(kotlin.math.abs(ratio(Color(0xFF777777), Color.White) - 4.48) < 0.01)
    }

    @Test
    fun dimmedContentIsDistinctFromRegularContent() {
        assertTrue(LightDimmedColors.content != LightColorScheme.onSurface)
        assertTrue(DarkDimmedColors.content != DarkColorScheme.onSurface)
        assertTrue(LightDimmedColors.container != LightColorScheme.surface)
        assertTrue(DarkDimmedColors.container != DarkColorScheme.surface)
    }
}
