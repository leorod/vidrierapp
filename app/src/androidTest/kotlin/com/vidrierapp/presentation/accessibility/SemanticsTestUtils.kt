package com.vidrierapp.presentation.accessibility

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role

fun hasStateDescription(value: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

fun hasLiveRegion(mode: LiveRegionMode): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, mode)

fun hasSemanticsRole(role: Role): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

fun isHeadingNode(): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
