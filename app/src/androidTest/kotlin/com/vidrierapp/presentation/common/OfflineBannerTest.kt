package com.vidrierapp.presentation.common

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OfflineBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun bannerShowsMessageAndVisitsActionWhenOffline() {
        composeTestRule.setContent {
            VidrierAppTheme { OfflineBannerSlot(visible = true, onVisitsClick = {}) }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.offline_banner_message)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.offline_banner_action)).assertIsDisplayed()
    }

    @Test
    fun bannerIsAbsentWhenOnline() {
        composeTestRule.setContent {
            VidrierAppTheme { OfflineBannerSlot(visible = false, onVisitsClick = {}) }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.offline_banner_message)).assertDoesNotExist()
    }

    @Test
    fun visitsActionInvokesCallbackAndMeetsTouchTarget() {
        var clicks = 0
        composeTestRule.setContent {
            VidrierAppTheme { OfflineBanner(onVisitsClick = { clicks++ }) }
        }

        val action = composeTestRule.onNodeWithText(context.getString(R.string.offline_banner_action))
        action.assertHeightIsAtLeast(48.dp)
        action.performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun bannerExposesAccessibleDescription() {
        composeTestRule.setContent {
            VidrierAppTheme { OfflineBanner(onVisitsClick = {}) }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.offline_banner_desc)).assertExists()
    }
}
