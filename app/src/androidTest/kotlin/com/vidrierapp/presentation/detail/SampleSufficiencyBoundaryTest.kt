package com.vidrierapp.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.Vote
import com.vidrierapp.presentation.map.ShopPreviewCard
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SampleSufficiencyBoundaryTest {

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
        totalVotes = totalVotes,
        voteBreakdown = listOf(
            Vote(category = Category(id = "cat-1", name = "Cafetería"), count = totalVotes, percentage = 100f)
        )
    )

    private fun detail(votes: Int) {
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

    private fun preview(votes: Int) {
        composeTestRule.setContent {
            VidrierAppTheme {
                ShopPreviewCard(shop = shop(votes), onViewDetailClick = {}, onDismiss = {})
            }
        }
    }

    private fun insufficientDetailText(votes: Int) =
        context.getString(R.string.detail_sample_insufficient, votes, MIN_VOTES_FOR_METRICS)

    private fun collectingPreviewText(votes: Int) =
        context.getString(R.string.map_marker_collecting, votes, MIN_VOTES_FOR_METRICS)

    @Test
    fun thresholdIsFifteen() {
        assertEquals(15, MIN_VOTES_FOR_METRICS)
    }

    @Test
    fun detailShowsInsufficientSampleNoticeWithZeroVotes() {
        detail(0)

        composeTestRule.onNodeWithText(insufficientDetailText(0)).assertIsDisplayed()
    }

    @Test
    fun detailShowsInsufficientSampleNoticeWithFourteenVotes() {
        detail(14)

        composeTestRule.onNodeWithText(insufficientDetailText(14)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.detail_votes_breakdown)).assertDoesNotExist()
    }

    @Test
    fun detailDoesNotShowInsufficientSampleNoticeWithFifteenVotes() {
        detail(15)

        composeTestRule.onNodeWithText(insufficientDetailText(15)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.detail_votes_breakdown)).assertIsDisplayed()
    }

    @Test
    fun detailDoesNotShowInsufficientSampleNoticeWithSixteenVotes() {
        detail(16)

        composeTestRule.onNodeWithText(insufficientDetailText(16)).assertDoesNotExist()
    }

    @Test
    fun previewShowsCollectingNoticeWithFourteenVotes() {
        preview(14)

        composeTestRule.onNodeWithText(collectingPreviewText(14)).assertIsDisplayed()
    }

    @Test
    fun previewDoesNotShowCollectingNoticeWithFifteenVotes() {
        preview(15)

        composeTestRule.onNodeWithText(collectingPreviewText(15)).assertDoesNotExist()
    }
}
