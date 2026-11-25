package com.vidrierapp.presentation.accessibility

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.Vote
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.presentation.common.ErrorState
import com.vidrierapp.presentation.common.OfflineBanner
import com.vidrierapp.presentation.detail.COLLECTING_SAMPLE_TAG
import com.vidrierapp.presentation.detail.ReportRentedButton
import com.vidrierapp.presentation.detail.ShopDetailContent
import com.vidrierapp.presentation.detail.ShopDetailUiState
import com.vidrierapp.presentation.map.CategoryFilterBar
import com.vidrierapp.presentation.map.SHOP_PREVIEW_CARD_TAG
import com.vidrierapp.presentation.map.ShopPreviewCard
import com.vidrierapp.presentation.theme.VidrierAppTheme
import com.vidrierapp.presentation.visit.LogVisitContent
import com.vidrierapp.presentation.visit.MyVisitsHubContent
import com.vidrierapp.presentation.visit.MyVisitsHubUiState
import com.vidrierapp.presentation.visit.SAVED_SHOP_CARD_TAG
import com.vidrierapp.presentation.visit.SavedShopItem
import com.vidrierapp.presentation.visit.VisitUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilitySemanticsTest {

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

    private fun detail(state: ShopDetailUiState) {
        composeTestRule.setContent {
            VidrierAppTheme {
                ShopDetailContent(
                    uiState = state,
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
    fun previewCardWithSufficientSampleIsMergedButtonWithHeadingAddress() {
        composeTestRule.setContent {
            VidrierAppTheme { ShopPreviewCard(shop = shop(34), onViewDetailClick = {}, onDismiss = {}) }
        }

        composeTestRule.onNodeWithTag(SHOP_PREVIEW_CARD_TAG)
            .assert(hasSemanticsRole(Role.Button))
            .assertTextContains("Av. Rivadavia 1000")
            .assertTextContains("Haedo")
            .assertTextContains(context.getString(R.string.map_card_total_votes, 34))
        composeTestRule.onNodeWithText("Av. Rivadavia 1000").assert(isHeadingNode())
    }

    @Test
    fun previewCardCollectingExposesStateDescription() {
        composeTestRule.setContent {
            VidrierAppTheme { ShopPreviewCard(shop = shop(6), onViewDetailClick = {}, onDismiss = {}) }
        }

        composeTestRule.onNodeWithTag(SHOP_PREVIEW_CARD_TAG)
            .assert(hasStateDescription(context.getString(R.string.map_card_state_collecting)))
            .assertTextContains(context.getString(R.string.map_marker_collecting, 6, MIN_VOTES_FOR_METRICS))
    }

    @Test
    fun chipsExposeActiveAndInactiveStateDescription() {
        composeTestRule.setContent {
            VidrierAppTheme {
                CategoryFilterBar(
                    categories = listOf(Category("cafe", "Cafeteria")),
                    selectedCategoryId = "cafe",
                    onCategorySelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cafeteria")
            .assert(hasStateDescription(context.getString(R.string.map_filter_active)))
        composeTestRule.onNodeWithText("Todos")
            .assert(hasStateDescription(context.getString(R.string.map_filter_inactive)))
    }

    @Test
    fun counterAnnouncesTotalAndOffersIncrementAndDecrementActions() {
        var increments = 0
        var decrements = 0
        composeTestRule.setContent {
            VidrierAppTheme {
                LogVisitContent(
                    uiState = VisitUiState(shopId = "shop-1", pedestrianCount = 3),
                    onBackClick = {},
                    onNotesChange = {},
                    onAddressChange = {},
                    onIncrementPedestrians = { increments++ },
                    onDecrementPedestrians = { decrements++ },
                    onSaveClick = {}
                )
            }
        }
        val total = context.resources.getQuantityString(R.plurals.visit_pedestrian_total_desc, 3, 3)

        val counter = composeTestRule.onNode(androidx.compose.ui.test.hasContentDescription(total))
        counter.assertContentDescriptionEquals(total)
        counter.assert(hasLiveRegion(LiveRegionMode.Polite))
        val actions = counter.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(
            listOf(
                context.getString(R.string.visit_pedestrian_increment_desc),
                context.getString(R.string.visit_pedestrian_decrement_desc)
            ),
            actions.map { it.label }
        )
        composeTestRule.runOnUiThread {
            actions.forEach { assertTrue(it.action()) }
        }
        assertEquals(1, increments)
        assertEquals(1, decrements)
    }

    @Test
    fun counterHeadingsAreMarked() {
        composeTestRule.setContent {
            VidrierAppTheme {
                LogVisitContent(
                    uiState = VisitUiState(shopId = "shop-1"),
                    onBackClick = {},
                    onNotesChange = {},
                    onAddressChange = {},
                    onIncrementPedestrians = {},
                    onDecrementPedestrians = {},
                    onSaveClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.visit_pedestrian_label)).assert(isHeadingNode())
        composeTestRule.onNodeWithText(context.getString(R.string.visit_photos_label))
            .performScrollTo().assert(isHeadingNode())
    }

    @Test
    fun offlineBannerIsMergedPoliteLiveRegion() {
        composeTestRule.setContent { VidrierAppTheme { OfflineBanner(onVisitsClick = {}) } }

        composeTestRule.onNode(hasText(context.getString(R.string.offline_banner_message)))
            .assert(hasLiveRegion(LiveRegionMode.Polite))
            .assertTextContains(context.getString(R.string.offline_banner_message))
    }

    @Test
    fun errorStateHasHeadingAndMergedLiveRegion() {
        composeTestRule.setContent { VidrierAppTheme { ErrorState(isOffline = true, onRetry = {}) } }

        composeTestRule.onNodeWithText(context.getString(R.string.status_error_title)).assert(isHeadingNode())
        composeTestRule.onNode(hasText(context.getString(R.string.status_error_offline_message)))
            .assert(hasLiveRegion(LiveRegionMode.Polite))
            .assertTextContains(context.getString(R.string.status_error_title))
    }

    @Test
    fun reportRentedButtonExposesStateDescription() {
        composeTestRule.setContent {
            VidrierAppTheme {
                ReportRentedButton(alreadyReported = true, isSubmitting = false, onClick = {})
            }
        }

        composeTestRule.onNode(hasText(context.getString(R.string.detail_btn_report_rented_done)))
            .assert(hasStateDescription(context.getString(R.string.detail_btn_report_rented_state_done)))
            .assertContentDescriptionEquals(context.getString(R.string.detail_btn_report_rented_done_desc))
    }

    @Test
    fun collectingCardMergesAndAnnouncesState() {
        detail(ShopDetailUiState.Success(shop(6), hasActivePass = true))

        composeTestRule.onNodeWithTag(COLLECTING_SAMPLE_TAG)
            .assert(hasStateDescription(context.getString(R.string.detail_sample_state_collecting)))
            .assertTextContains(context.getString(R.string.detail_sample_insufficient, 6, MIN_VOTES_FOR_METRICS))
    }

    @Test
    fun paywallCardMergesTitleDescriptionAndLockedState() {
        detail(ShopDetailUiState.Success(shop(20), hasActivePass = false))

        composeTestRule.onNodeWithText(context.getString(R.string.pass_locked_title)).assert(isHeadingNode())
        composeTestRule.onNode(hasText(context.getString(R.string.pass_locked_description)))
            .assert(hasStateDescription(context.getString(R.string.pass_locked_state)))
            .assertTextContains(context.getString(R.string.pass_locked_title))
    }

    @Test
    fun metricBlocksHaveHeadingsAndBarDescriptions() {
        detail(ShopDetailUiState.Success(shop(20), hasActivePass = true))

        composeTestRule.onNodeWithText(context.getString(R.string.detail_votes_breakdown))
            .performScrollTo().assert(isHeadingNode())
        composeTestRule.onNode(
            androidx.compose.ui.test.hasContentDescription(
                context.getString(R.string.detail_category_bar_desc, "Cafetería", 20, 100f)
            )
        ).performScrollTo().assertTextContains("Cafetería")
    }

    @Test
    fun hubItemsAreMergedWithRoleAndHeadings() {
        composeTestRule.setContent {
            VidrierAppTheme {
                MyVisitsHubContent(
                    uiState = MyVisitsHubUiState.Success(
                        visits = listOf(
                            Visit(id = "v1", shopId = null, manualAddress = "Corrientes 500", timestamp = 1_690_000_000_000L, notes = "vereda ancha", pedestrianCount = 4)
                        ),
                        savedShops = listOf(SavedShopItem("shop-1", "Av. Santa Fe 1234", "Palermo", 3))
                    ),
                    onNewVisitClick = {},
                    onSavedShopClick = {},
                    onRetryClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.hub_visits_section)).assert(isHeadingNode())
        composeTestRule.onNode(hasText(context.getString(R.string.visit_manual_address_target, "Corrientes 500")))
            .assertTextContains("vereda ancha")
            .assertTextContains(context.resources.getQuantityString(R.plurals.visit_pedestrian_count, 4, 4))
        composeTestRule.onNodeWithTag(SAVED_SHOP_CARD_TAG).performScrollTo()
            .assert(hasSemanticsRole(Role.Button))
            .assert(hasStateDescription(context.getString(R.string.hub_saved_cached_label)))
            .assertContentDescriptionEquals(context.getString(R.string.hub_saved_shop_desc, "Av. Santa Fe 1234"))
    }
}
