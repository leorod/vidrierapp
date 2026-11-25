package com.vidrierapp.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class DimmedColors(
    val content: Color,
    val container: Color,
    val outline: Color
)

internal val LightDimmedColors = DimmedColors(
    content = dimmedContentLight,
    container = dimmedContainerLight,
    outline = dimmedOutlineLight
)

internal val DarkDimmedColors = DimmedColors(
    content = dimmedContentDark,
    container = dimmedContainerDark,
    outline = dimmedOutlineDark
)

val LocalDimmedColors = staticCompositionLocalOf { LightDimmedColors }

val MaterialTheme.dimmed: DimmedColors
    @Composable
    get() = LocalDimmedColors.current

@Composable
fun DimmedContent(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MaterialTheme.dimmed
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.content,
        border = BorderStroke(1.dp, colors.outline)
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
