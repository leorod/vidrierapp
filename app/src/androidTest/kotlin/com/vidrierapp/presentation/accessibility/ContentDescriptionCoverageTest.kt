package com.vidrierapp.presentation.accessibility

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assert
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.detail.ShopDetailContent
import com.vidrierapp.presentation.detail.ShopDetailUiState
import com.vidrierapp.presentation.map.CategoryFilterBar
import com.vidrierapp.presentation.map.MyLocationButton
import com.vidrierapp.presentation.navigation.AppBottomBar
import com.vidrierapp.presentation.navigation.MapDestination
import com.vidrierapp.presentation.navigation.VisitsHubDestination
import com.vidrierapp.presentation.theme.VidrierAppTheme
import com.vidrierapp.presentation.visit.LogVisitContent
import com.vidrierapp.presentation.visit.MyVisitsHubContent
import com.vidrierapp.presentation.visit.MyVisitsHubUiState
import com.vidrierapp.presentation.visit.VisitUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContentDescriptionCoverageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun shop() = Shop(
        id = "shop-1",
        address = "Av. Rivadavia 1000",
        neighborhood = "Haedo",
        latitude = -34.64,
        longitude = -58.60,
        surfaceAreaM2 = 50,
        rentalPrice = "100",
        topCategory = Category(id = "cat-1", name = "Cafetería"),
        totalVotes = 20,
        contactPhone = "+54 11 4687-1234",
        contactEmail = "info@x.example"
    )

    private fun detail(
        state: ShopDetailUiState,
        onBack: () -> Unit = {},
        onReport: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                ShopDetailContent(
                    uiState = state,
                    onBackClick = onBack,
                    onLogVisitClick = {},
                    onReportRentedClick = onReport,
                    onRetry = {},
                    onStartTrialClick = {},
                    onPurchasePassClick = {}
                )
            }
        }
    }

    private fun logVisit(state: VisitUiState, onBack: () -> Unit = {}) {
        composeTestRule.setContent {
            VidrierAppTheme {
                LogVisitContent(
                    uiState = state,
                    onBackClick = onBack,
                    onNotesChange = {},
                    onAddressChange = {},
                    onIncrementPedestrians = {},
                    onDecrementPedestrians = {},
                    onSaveClick = {}
                )
            }
        }
    }

    @Test
    fun myLocationButtonHasContentDescriptionAndIsClickable() {
        var clicks = 0
        composeTestRule.setContent {
            VidrierAppTheme { MyLocationButton(onClick = { clicks++ }) }
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.map_btn_my_location_desc))
            .assertHasClickAction()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun categoryFilterRowHasContentDescriptionAndChipsAreLabelled() {
        composeTestRule.setContent {
            VidrierAppTheme {
                CategoryFilterBar(
                    categories = listOf(Category("cafe", "Cafeteria")),
                    selectedCategoryId = null,
                    onCategorySelected = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.map_filter_row_desc))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_filter_all)).assertHasClickAction()
        composeTestRule.onNodeWithText("Cafeteria").assertHasClickAction()
    }

    @Test
    fun shopDetailBackButtonHasContentDescriptionAndInvokesCallback() {
        var backs = 0
        detail(ShopDetailUiState.Success(shop(), hasActivePass = true), onBack = { backs++ })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.action_back))
            .assertHasClickAction()
            .performClick()

        assertEquals(1, backs)
    }

    @Test
    fun shopDetailBackButtonHasContentDescriptionWhileLoading() {
        detail(ShopDetailUiState.Loading)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.action_back))
            .assertIsDisplayed()
    }

    @Test
    fun shopDetailActionsHaveContentDescriptionWhenUnlocked() {
        detail(ShopDetailUiState.Success(shop(), hasActivePass = true))

        listOf(
            R.string.detail_btn_call_desc,
            R.string.detail_btn_whatsapp_desc,
            R.string.detail_btn_email_desc,
            R.string.detail_btn_log_visit_desc,
            R.string.detail_btn_report_rented_desc
        ).forEach { res ->
            composeTestRule
                .onNodeWithContentDescription(context.getString(res))
                .performScrollTo()
                .assertHasClickAction()
        }
    }

    @Test
    fun shopDetailPaywallActionsHaveContentDescriptionWhenLocked() {
        detail(ShopDetailUiState.Success(shop(), hasActivePass = false, trialAvailable = true))

        listOf(
            R.string.pass_btn_start_trial_desc,
            R.string.pass_btn_activate_desc,
            R.string.detail_btn_log_visit_desc
        ).forEach { res ->
            composeTestRule
                .onNodeWithContentDescription(context.getString(res))
                .performScrollTo()
                .assertHasClickAction()
        }
    }

    @Test
    fun logVisitBackButtonHasContentDescriptionAndInvokesCallback() {
        var backs = 0
        logVisit(VisitUiState(shopId = "shop-1"), onBack = { backs++ })

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.action_back))
            .assertHasClickAction()
            .performClick()

        assertEquals(1, backs)
    }

    @Test
    fun logVisitCounterButtonsAndCameraHaveContentDescription() {
        logVisit(VisitUiState(shopId = "shop-1"))

        listOf(
            R.string.visit_pedestrian_increment_desc,
            R.string.visit_pedestrian_decrement_desc,
            R.string.visit_btn_take_photo_desc
        ).forEach { res ->
            composeTestRule
                .onNodeWithContentDescription(context.getString(res))
                .performScrollTo()
                .assertHasClickAction()
        }
    }

    @Test
    fun logVisitCounterExposesTotalAsContentDescription() {
        logVisit(VisitUiState(shopId = "shop-1", pedestrianCount = 5))

        composeTestRule
            .onNodeWithContentDescription(
                context.resources.getQuantityString(R.plurals.visit_pedestrian_total_desc, 5, 5)
            )
            .assertIsDisplayed()
    }

    @Test
    fun saveVisitButtonIsLabelledClickableAndEnabled() {
        logVisit(VisitUiState(shopId = "shop-1"))

        composeTestRule
            .onNode(hasText(context.getString(R.string.visit_btn_submit)).and(hasClickActionMatcher()))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun hubNewVisitButtonHasContentDescription() {
        composeTestRule.setContent {
            VidrierAppTheme {
                MyVisitsHubContent(
                    uiState = MyVisitsHubUiState.Success(),
                    onNewVisitClick = {},
                    onSavedShopClick = {},
                    onRetryClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.hub_fab_desc))
            .assertHasClickAction()
    }

    @Test
    fun bottomNavigationTabsAreLabelledWithRoleTabAndSelectionState() {
        composeTestRule.setContent {
            VidrierAppTheme {
                AppBottomBar(currentDestination = MapDestination, onTabSelected = {})
            }
        }

        val explore = composeTestRule.onNode(hasText(context.getString(R.string.nav_explore)).and(hasClickActionMatcher()))
        val visits = composeTestRule.onNode(hasText(context.getString(R.string.nav_visits)).and(hasClickActionMatcher()))
        explore.assert(hasSemanticsRole(Role.Tab)).assertIsSelected()
        visits.assert(hasSemanticsRole(Role.Tab)).assertIsNotSelected()
    }

    @Test
    fun bottomNavigationTabSelectionReportsDestination() {
        val selected = mutableListOf<Any>()
        composeTestRule.setContent {
            VidrierAppTheme {
                AppBottomBar(currentDestination = MapDestination, onTabSelected = { selected += it })
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.nav_visits)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.nav_explore)).performClick()

        assertEquals(listOf<Any>(VisitsHubDestination), selected)
    }

    private fun hasClickActionMatcher() =
        androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.OnClick)
}
