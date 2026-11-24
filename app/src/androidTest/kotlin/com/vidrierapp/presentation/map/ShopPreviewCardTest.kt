package com.vidrierapp.presentation.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShopPreviewCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun shop(totalVotes: Int) = Shop(
        id = "shop-1",
        address = "Av. Rivadavia 1000",
        neighborhood = "Haedo",
        latitude = -34.64,
        longitude = -58.60,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = Category(id = "cat-1", name = "Cafetería"),
        totalVotes = totalVotes
    )

    private fun setContent(
        shop: Shop,
        onViewDetailClick: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                ShopPreviewCard(
                    shop = shop,
                    onViewDetailClick = onViewDetailClick,
                    onDismiss = onDismiss
                )
            }
        }
    }

    @Test
    fun sufficientSampleShowsCategoryAndVotes() {
        setContent(shop(totalVotes = 34))

        composeTestRule.onNodeWithText("Av. Rivadavia 1000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Haedo").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("${context.getString(R.string.map_card_top_rubro)} Cafetería")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.map_card_total_votes, 34))
            .assertIsDisplayed()
    }

    @Test
    fun insufficientSampleShowsCollectingNoticeAndHidesCategory() {
        setContent(shop(totalVotes = 6))

        composeTestRule
            .onNodeWithText(context.getString(R.string.map_marker_collecting, 6, MIN_VOTES_FOR_METRICS))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.map_card_top_rubro), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun viewDetailButtonInvokesCallback() {
        var clicks = 0
        setContent(shop(totalVotes = 34), onViewDetailClick = { clicks++ })

        composeTestRule
            .onNodeWithText(context.getString(R.string.map_card_view_detail))
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun closeButtonInvokesOnDismiss() {
        var dismissed = 0
        setContent(shop(totalVotes = 34), onDismiss = { dismissed++ })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.action_close))
            .performClick()

        assertEquals(1, dismissed)
    }

    @Test
    fun swipeDownInvokesOnDismiss() {
        var dismissed = 0
        setContent(shop(totalVotes = 34), onDismiss = { dismissed++ })

        composeTestRule.onNodeWithTag(SHOP_PREVIEW_CARD_TAG).performTouchInput { swipeDown() }

        assertEquals(1, dismissed)
    }
}
