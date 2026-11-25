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

class ErrorStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun retryButtonInvokesCallbackAndMeetsTouchTarget() {
        var retries = 0
        composeTestRule.setContent {
            VidrierAppTheme { ErrorState(isOffline = false, onRetry = { retries++ }) }
        }

        val retry = composeTestRule.onNodeWithText(context.getString(R.string.action_retry))
        retry.assertIsDisplayed()
        retry.assertHeightIsAtLeast(48.dp)
        retry.performClick()

        assertEquals(1, retries)
    }

    @Test
    fun offlineVariantShowsOfflineMessage() {
        composeTestRule.setContent {
            VidrierAppTheme { ErrorState(isOffline = true, onRetry = {}) }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.status_error_offline_message)).assertIsDisplayed()
    }

    @Test
    fun genericVariantShowsServerMessage() {
        composeTestRule.setContent {
            VidrierAppTheme { ErrorState(isOffline = false, onRetry = {}) }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.status_error_message)).assertIsDisplayed()
    }

    @Test
    fun exposesAccessibleDescriptions() {
        composeTestRule.setContent {
            VidrierAppTheme { ErrorState(isOffline = false, onRetry = {}) }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.action_retry_desc)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.status_error_title)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.status_error_message)).assertExists()
    }
}
