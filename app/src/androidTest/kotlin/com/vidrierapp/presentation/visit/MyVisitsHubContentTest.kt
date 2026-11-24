package com.vidrierapp.presentation.visit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyVisitsHubContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        state: MyVisitsHubUiState,
        onSavedShopClick: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                MyVisitsHubContent(
                    uiState = state,
                    onNewVisitClick = {},
                    onSavedShopClick = onSavedShopClick,
                    onRetryClick = {}
                )
            }
        }
    }

    private val shopVisit = Visit(
        id = "v1",
        shopId = "shop-1",
        timestamp = 1_700_000_000_000L,
        notes = "vereda ancha",
        pedestrianCount = 12
    )
    private val manualVisit = Visit(
        id = "v2",
        shopId = null,
        manualAddress = "Corrientes 500",
        timestamp = 1_690_000_000_000L,
        notes = "",
        pedestrianCount = 1
    )

    @Test
    fun visitCardShowsResolvedShopNameNotRawId() {
        setContent(
            MyVisitsHubUiState.Success(
                visits = listOf(shopVisit),
                shopNames = mapOf("shop-1" to "Av. Santa Fe 1234")
            )
        )

        composeTestRule.onNodeWithText(context.getString(R.string.visit_shop_target, "Av. Santa Fe 1234"))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("vereda ancha").assertIsDisplayed()
        composeTestRule.onNodeWithText(formatVisitDate(shopVisit.timestamp)).assertIsDisplayed()
    }

    @Test
    fun visitWithUncachedShopShowsNeutralLabel() {
        setContent(MyVisitsHubUiState.Success(visits = listOf(shopVisit)))

        composeTestRule.onNodeWithText(
            context.getString(R.string.visit_shop_target, context.getString(R.string.hub_visit_shop_unknown))
        ).assertIsDisplayed()
    }

    @Test
    fun manualAddressVisitShowsAddress() {
        setContent(MyVisitsHubUiState.Success(visits = listOf(manualVisit)))

        composeTestRule.onNodeWithText(context.getString(R.string.visit_manual_address_target, "Corrientes 500"))
            .assertIsDisplayed()
    }

    @Test
    fun savedShopShowsAgeAndInvokesCallback() {
        var clicked: String? = null
        setContent(
            MyVisitsHubUiState.Success(
                savedShops = listOf(
                    SavedShopItem("shop-1", "Av. Santa Fe 1234", "Palermo", 3),
                    SavedShopItem("shop-2", "Corrientes 500", "Centro", 0)
                )
            ),
            onSavedShopClick = { clicked = it }
        )

        composeTestRule.onNodeWithText(context.resources.getQuantityString(R.plurals.hub_saved_days_ago, 3, 3))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.hub_saved_today))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.hub_saved_shop_desc, "Av. Santa Fe 1234"))
            .performScrollTo().performClick()

        assertEquals("shop-1", clicked)
    }

    @Test
    fun singularDayIsUsedForOneDay() {
        setContent(
            MyVisitsHubUiState.Success(savedShops = listOf(SavedShopItem("s", "Calle 1", "", 1)))
        )

        composeTestRule.onNodeWithText("Guardado hace 1 día").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun emptyStatesAreShown() {
        setContent(MyVisitsHubUiState.Success())

        composeTestRule.onNodeWithText(context.getString(R.string.hub_empty)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.hub_saved_shops_empty))
            .performScrollTo().assertIsDisplayed()
    }
}
