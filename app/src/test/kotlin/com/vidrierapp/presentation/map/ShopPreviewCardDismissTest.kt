package com.vidrierapp.presentation.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopPreviewCardDismissTest {

    @Test
    fun dragBelowThresholdDoesNotDismiss() {
        assertFalse(shouldDismissOnDrag(dragDistancePx = 50f, thresholdPx = 100f))
    }

    @Test
    fun dragAtThresholdDismisses() {
        assertTrue(shouldDismissOnDrag(dragDistancePx = 100f, thresholdPx = 100f))
    }

    @Test
    fun dragPastThresholdDismisses() {
        assertTrue(shouldDismissOnDrag(dragDistancePx = 250f, thresholdPx = 100f))
    }

    @Test
    fun upwardDragDoesNotDismiss() {
        assertFalse(shouldDismissOnDrag(dragDistancePx = -200f, thresholdPx = 100f))
    }
}
