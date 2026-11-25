package com.vidrierapp.presentation.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.common.ErrorState
import com.vidrierapp.presentation.common.OfflineBanner
import com.vidrierapp.presentation.detail.ContactActions
import com.vidrierapp.presentation.detail.ReportRentedButton
import com.vidrierapp.presentation.map.CategoryFilterBar
import com.vidrierapp.presentation.map.SHOP_PREVIEW_CARD_TAG
import com.vidrierapp.presentation.map.ShopPreviewCard
import com.vidrierapp.presentation.visit.MyVisitsHubContent
import com.vidrierapp.presentation.visit.MyVisitsHubUiState
import com.vidrierapp.presentation.visit.SAVED_SHOP_CARD_TAG
import com.vidrierapp.presentation.visit.SavedShopItem
import org.junit.Rule
import org.junit.Test

class TouchTargetSizesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val shop = Shop(
        id = "shop-1",
        address = "Av. Rivadavia 1000",
        neighborhood = "Haedo",
        latitude = -34.64,
        longitude = -58.60,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = Category(id = "cat-1", name = "Cafeteria"),
        totalVotes = 20,
        contactPhone = "+5491122334455",
        contactEmail = "a@b.com"
    )

    private fun SemanticsNodeInteraction.assertMinTouchTarget() {
        assertWidthIsAtLeast(48.dp)
        assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun previewCardControlsMeetMinimum() {
        composeTestRule.setContent {
            VidrierAppTheme { ShopPreviewCard(shop = shop, onViewDetailClick = {}, onDismiss = {}) }
        }

        composeTestRule
            .onNode(
                hasClickAction() and
                    !hasTestTag(SHOP_PREVIEW_CARD_TAG) and
                    hasAnyDescendant(hasContentDescription(context.getString(R.string.action_close))),
                useUnmergedTree = true
            )
            .assertMinTouchTarget()
        composeTestRule.onNode(
                hasClickAction() and
                    !hasTestTag(SHOP_PREVIEW_CARD_TAG) and
                    hasAnyDescendant(hasText(context.getString(R.string.map_card_view_detail))),
                useUnmergedTree = true
            ).assertMinTouchTarget()
    }

    @Test
    fun categoryChipsMeetMinimum() {
        composeTestRule.setContent {
            VidrierAppTheme {
                CategoryFilterBar(
                    categories = listOf(Category("a", "A")),
                    selectedCategoryId = null,
                    onCategorySelected = {}
                )
            }
        }

        composeTestRule.onNode(hasText(context.getString(R.string.map_filter_all)) and hasClickAction()).assertMinTouchTarget()
        composeTestRule.onNode(hasText("A") and hasClickAction()).assertMinTouchTarget()
    }

    @Test
    fun bannerAndErrorButtonsMeetMinimum() {
        composeTestRule.setContent {
            VidrierAppTheme {
                Column {
                    OfflineBanner(onVisitsClick = {})
                    ErrorState(isOffline = false, onRetry = {})
                }
            }
        }

        composeTestRule.onNode(hasText(context.getString(R.string.offline_banner_action)) and hasClickAction()).assertMinTouchTarget()
        composeTestRule.onNode(hasText(context.getString(R.string.action_retry)) and hasClickAction()).assertMinTouchTarget()
    }

    @Test
    fun contactAndReportButtonsMeetMinimum() {
        composeTestRule.setContent {
            VidrierAppTheme {
                Column {
                    ContactActions(shop = shop, onCallClick = {}, onWhatsAppClick = {}, onEmailClick = {})
                    ReportRentedButton(alreadyReported = false, isSubmitting = false, onClick = {})
                }
            }
        }

        composeTestRule.onNode(hasText(context.getString(R.string.detail_btn_call)) and hasClickAction()).assertMinTouchTarget()
        composeTestRule.onNode(hasText(context.getString(R.string.detail_btn_whatsapp)) and hasClickAction()).assertMinTouchTarget()
        composeTestRule.onNode(hasText(context.getString(R.string.detail_btn_email)) and hasClickAction()).assertMinTouchTarget()
        composeTestRule.onNode(hasText(context.getString(R.string.detail_btn_report_rented)) and hasClickAction()).assertMinTouchTarget()
    }

    @Test
    fun hubNewVisitAndSavedShopMeetMinimum() {
        composeTestRule.setContent {
            VidrierAppTheme {
                MyVisitsHubContent(
                    uiState = MyVisitsHubUiState.Success(
                        savedShops = listOf(SavedShopItem("s1", "Av. Santa Fe 1", "Palermo", 2))
                    ),
                    onNewVisitClick = {},
                    onSavedShopClick = {},
                    onRetryClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(SAVED_SHOP_CARD_TAG).assertMinTouchTarget()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.hub_fab_desc)).assertMinTouchTarget()
    }
}
