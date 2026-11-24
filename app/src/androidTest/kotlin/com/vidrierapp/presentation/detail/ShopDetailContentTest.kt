package com.vidrierapp.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.Vote
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShopDetailContentTest {

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

    private fun setContent(
        state: ShopDetailUiState,
        onLogVisitClick: (String) -> Unit = {},
        onStartTrialClick: () -> Unit = {},
        onPurchasePassClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                ShopDetailContent(
                    uiState = state,
                    onBackClick = {},
                    onLogVisitClick = onLogVisitClick,
                    onReportRentedClick = {},
                    onRetry = {},
                    onStartTrialClick = onStartTrialClick,
                    onPurchasePassClick = onPurchasePassClick
                )
            }
        }
    }

    @Test
    fun insufficientSampleShowsCardAndHidesBreakdown() {
        setContent(ShopDetailUiState.Success(shop(totalVotes = 6), hasActivePass = true))

        composeTestRule
            .onNodeWithText(
                context.getString(R.string.detail_sample_insufficient, 6, MIN_VOTES_FOR_METRICS)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.detail_votes_breakdown))
            .assertDoesNotExist()
    }

    @Test
    fun sufficientSampleWithPassShowsBreakdown() {
        setContent(ShopDetailUiState.Success(shop(totalVotes = 20), hasActivePass = true))

        composeTestRule
            .onNodeWithText(context.getString(R.string.detail_votes_breakdown))
            .assertIsDisplayed()
    }

    @Test
    fun withoutPassShowsLockedCardAndHidesBreakdown() {
        setContent(ShopDetailUiState.Success(shop(totalVotes = 20), hasActivePass = false))

        composeTestRule
            .onNodeWithText(context.getString(R.string.pass_locked_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.detail_votes_breakdown))
            .assertDoesNotExist()
    }

    @Test
    fun lockedFichaKeepsQualitativeInfoAndHidesVoteCount() {
        setContent(ShopDetailUiState.Success(shop(totalVotes = 20), hasActivePass = false))

        composeTestRule.onNodeWithText("Av. Rivadavia 1000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Haedo").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.detail_sample_votes, 20))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.pass_simulation_notice))
            .assertIsDisplayed()
    }

    @Test
    fun unlockedFichaShowsVoteCountAndNoPaywall() {
        setContent(ShopDetailUiState.Success(shop(totalVotes = 20), hasActivePass = true))

        composeTestRule
            .onNodeWithText(context.getString(R.string.detail_sample_votes, 20))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.pass_locked_title))
            .assertDoesNotExist()
    }

    @Test
    fun startTrialButtonInvokesCallback() {
        var clicks = 0
        setContent(
            ShopDetailUiState.Success(shop(totalVotes = 20), hasActivePass = false),
            onStartTrialClick = { clicks++ }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.pass_btn_start_trial))
            .performScrollTo()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun purchaseButtonInvokesCallback() {
        var clicks = 0
        setContent(
            ShopDetailUiState.Success(shop(totalVotes = 20), hasActivePass = false),
            onPurchasePassClick = { clicks++ }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.pass_btn_activate))
            .performScrollTo()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun trialButtonIsHiddenWhenTrialAlreadyUsed() {
        setContent(
            ShopDetailUiState.Success(
                shop(totalVotes = 20),
                hasActivePass = false,
                trialAvailable = false
            )
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.pass_btn_start_trial))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.pass_btn_activate))
            .assertIsDisplayed()
    }

    @Test
    fun logVisitButtonInvokesCallbackWithShopId() {
        var clickedShopId: String? = null
        setContent(
            ShopDetailUiState.Success(shop(totalVotes = 20), hasActivePass = true),
            onLogVisitClick = { clickedShopId = it }
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.detail_btn_log_visit))
            .performScrollTo()
            .performClick()

        assertEquals("shop-1", clickedShopId)
    }
}
