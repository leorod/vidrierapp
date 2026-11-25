package com.vidrierapp.presentation.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.common.ErrorState
import com.vidrierapp.presentation.common.OfflineBanner
import com.vidrierapp.presentation.detail.ShopDetailContent
import com.vidrierapp.presentation.detail.ShopDetailUiState
import com.vidrierapp.presentation.map.CategoryFilterBar
import com.vidrierapp.presentation.map.ShopPreviewCard
import com.vidrierapp.presentation.theme.VidrierAppTheme
import com.vidrierapp.presentation.visit.LogVisitContent
import com.vidrierapp.presentation.visit.MyVisitsHubContent
import com.vidrierapp.presentation.visit.MyVisitsHubUiState
import com.vidrierapp.presentation.visit.SavedShopItem
import com.vidrierapp.presentation.visit.VisitUiState
import com.vidrierapp.domain.model.Visit
import org.junit.Rule
import org.junit.Test

class FontScaleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun scaled(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2.0f)) {
                VidrierAppTheme { content() }
            }
        }
    }

    private fun shop(totalVotes: Int) = Shop(
        id = "shop-1",
        address = "Av. Rivadavia 1000, Ramos Mejía, Buenos Aires",
        neighborhood = "Haedo",
        latitude = -34.64,
        longitude = -58.60,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = Category(id = "cat-1", name = "Cafetería"),
        totalVotes = totalVotes
    )

    @Test
    fun previewCardKeepsKeyNodesAtDoubleFontScale() {
        scaled { ShopPreviewCard(shop = shop(6), onViewDetailClick = {}, onDismiss = {}) }

        composeTestRule.onNodeWithText("Av. Rivadavia 1000, Ramos Mejía, Buenos Aires").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_marker_collecting, 6, MIN_VOTES_FOR_METRICS))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_card_view_detail))
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun chipsKeepMinimumHeightAtDoubleFontScale() {
        scaled {
            CategoryFilterBar(
                categories = listOf(Category("cafe", "Cafeteria")),
                selectedCategoryId = null,
                onCategorySelected = {}
            )
        }

        composeTestRule.onNodeWithText("Todos").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Cafeteria").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun offlineBannerAndErrorStateKeepNodesAtDoubleFontScale() {
        scaled { OfflineBanner(onVisitsClick = {}) }

        composeTestRule.onNodeWithText(context.getString(R.string.offline_banner_message)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.offline_banner_action))
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun errorStateKeepsNodesAtDoubleFontScale() {
        scaled { ErrorState(isOffline = false, onRetry = {}) }

        composeTestRule.onNodeWithText(context.getString(R.string.status_error_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_retry))
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun fichaWithPaywallKeepsNodesReachableAtDoubleFontScale() {
        scaled {
            ShopDetailContent(
                uiState = ShopDetailUiState.Success(shop(20), hasActivePass = false),
                onBackClick = {},
                onLogVisitClick = {},
                onReportRentedClick = {},
                onRetry = {},
                onStartTrialClick = {},
                onPurchasePassClick = {}
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.pass_locked_title))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.pass_btn_activate))
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_log_visit))
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_report_rented))
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun contactButtonsWrapInsteadOfCollapsingAtDoubleFontScale() {
        scaled {
            ShopDetailContent(
                uiState = ShopDetailUiState.Success(
                    shop(20).copy(contactPhone = "+5491100000000", contactEmail = "a@b.com"),
                    hasActivePass = true
                ),
                onBackClick = {},
                onLogVisitClick = {},
                onReportRentedClick = {},
                onRetry = {},
                onStartTrialClick = {},
                onPurchasePassClick = {}
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_email))
            .performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_email))
            .assertHeightIsEqualTo(
                composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_call))
                    .getBoundsInRoot().height
            )
    }

    @Test
    fun logVisitKeepsNodesReachableAtDoubleFontScale() {
        scaled {
            LogVisitContent(
                uiState = VisitUiState(shopId = "shop-1", pedestrianCount = 128),
                onBackClick = {},
                onNotesChange = {},
                onAddressChange = {},
                onIncrementPedestrians = {},
                onDecrementPedestrians = {},
                onSaveClick = {}
            )
        }

        composeTestRule.onNodeWithText("128").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.visit_btn_take_photo))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.visit_btn_submit))
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun hubKeepsNodesReachableAtDoubleFontScale() {
        scaled {
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

        composeTestRule.onNodeWithText(context.getString(R.string.hub_visits_section)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.hub_saved_shops_section))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNode(hasText("Av. Santa Fe 1234")).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.hub_fab_desc))
            .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }
}
