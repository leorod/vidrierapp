package com.vidrierapp.presentation.theme

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val MinTouchTarget = 48.dp

fun Modifier.minTouchTarget(): Modifier =
    sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
