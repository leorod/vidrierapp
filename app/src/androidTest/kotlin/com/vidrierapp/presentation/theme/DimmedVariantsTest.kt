package com.vidrierapp.presentation.theme

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.detail.COLLECTING_SAMPLE_TAG
import com.vidrierapp.presentation.detail.ShopDetailContent
import com.vidrierapp.presentation.detail.ShopDetailUiState
import com.vidrierapp.presentation.map.PREVIEW_COLLECTING_TAG
import com.vidrierapp.presentation.map.ShopPreviewCard
import com.vidrierapp.presentation.visit.MyVisitsHubContent
import com.vidrierapp.presentation.visit.MyVisitsHubUiState
import com.vidrierapp.presentation.visit.SAVED_SHOP_CARD_TAG
import com.vidrierapp.presentation.visit.SavedShopItem
import org.junit.Rule
import org.junit.Test

class DimmedVariantsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun shop(votes: Int) = Shop(
        id = "shop-1",
        address = "Av. Rivadavia 1000",
        neighborhood = "Haedo",
        latitude = -34.64,
        longitude = -58.60,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = Category(id = "cat-1", name = "Cafeteria"),
        totalVotes = votes
    )

    private fun setDetail(votes: Int) {
        composeTestRule.setContent {
            VidrierAppTheme {
                ShopDetailContent(
                    uiState = ShopDetailUiState.Success(shop(votes), hasActivePass = true),
                    onBackClick = {},
                    onLogVisitClick = {},
                    onReportRentedClick = {},
                    onRetry = {},
                    onStartTrialClick = {},
                    onPurchasePassClick = {}
                )
            }
        }
    }

    @Test
    fun fichaWithFewVotesShowsDimmedCardWithLabel() {
        setDetail(6)

        composeTestRule.onNodeWithTag(COLLECTING_SAMPLE_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.detail_sample_insufficient, 6, MIN_VOTES_FOR_METRICS))
            .assertIsDisplayed()
    }

    @Test
    fun fichaWithSufficientVotesHasNoDimmedCard() {
        setDetail(20)

        composeTestRule.onNodeWithTag(COLLECTING_SAMPLE_TAG).assertDoesNotExist()
    }

    @Test
    fun previewCardCollectingIsDimmedWithLabelAndSufficientIsNot() {
        composeTestRule.setContent {
            VidrierAppTheme { ShopPreviewCard(shop = shop(4), onViewDetailClick = {}, onDismiss = {}) }
        }

        composeTestRule.onNodeWithTag(PREVIEW_COLLECTING_TAG, useUnmergedTree = true).assertExists()
        composeTestRule
            .onNodeWithText(context.getString(R.string.map_marker_collecting, 4, MIN_VOTES_FOR_METRICS))
            .assertIsDisplayed()
    }

    @Test
    fun previewCardSufficientIsNotDimmed() {
        composeTestRule.setContent {
            VidrierAppTheme { ShopPreviewCard(shop = shop(30), onViewDetailClick = {}, onDismiss = {}) }
        }

        composeTestRule.onNodeWithTag(PREVIEW_COLLECTING_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun savedShopCardShowsCachedLabelWithAge() {
        composeTestRule.setContent {
            VidrierAppTheme {
                MyVisitsHubContent(
                    uiState = MyVisitsHubUiState.Success(
                        savedShops = listOf(SavedShopItem("s1", "Av. Santa Fe 1", "Palermo", 0))
                    ),
                    onNewVisitClick = {},
                    onSavedShopClick = {},
                    onRetryClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(SAVED_SHOP_CARD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.hub_saved_cached_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.hub_saved_today)).assertIsDisplayed()
    }
}
