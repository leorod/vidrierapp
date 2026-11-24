package com.vidrierapp.presentation.visit

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.vidrierapp.domain.model.Visit
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LogVisitContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        state: VisitUiState,
        onNotes: (String) -> Unit = {},
        onAddress: (String) -> Unit = {},
        onSave: () -> Unit = {},
        onIncrement: () -> Unit = {},
        onDecrement: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                LogVisitContent(
                    uiState = state,
                    onBackClick = {},
                    onNotesChange = onNotes,
                    onAddressChange = onAddress,
                    onIncrementPedestrians = onIncrement,
                    onDecrementPedestrians = onDecrement,
                    onSaveClick = onSave
                )
            }
        }
    }

    @Test
    fun addressFieldIsShownWhenThereIsNoShop() {
        setContent(VisitUiState())

        composeTestRule.onNodeWithText(context.getString(R.string.visit_address_label)).assertIsDisplayed()
    }

    @Test
    fun addressFieldIsHiddenForKnownShop() {
        setContent(VisitUiState(shopId = "shop-1"))

        composeTestRule
            .onAllNodesWithText(context.getString(R.string.visit_address_label))
            .assertCountEquals(0)
        composeTestRule
            .onNodeWithText(context.getString(R.string.visit_shop_target, "shop-1"))
            .assertIsDisplayed()
    }

    @Test
    fun addressErrorIsDisplayed() {
        setContent(VisitUiState(showAddressError = true))

        composeTestRule.onNodeWithText(context.getString(R.string.visit_address_required)).assertIsDisplayed()
    }

    @Test
    fun typingNotesAndTappingSaveInvokeCallbacks() {
        var notes = ""
        var saves = 0
        setContent(VisitUiState(shopId = "shop-1"), onNotes = { notes = it }, onSave = { saves++ })

        composeTestRule.onNodeWithText(context.getString(R.string.visit_notes_label)).performTextInput("hola")
        composeTestRule.onNodeWithText(context.getString(R.string.visit_btn_submit)).performClick()

        assertEquals("hola", notes)
        assertEquals(1, saves)
    }

    @Test
    fun incrementButtonInvokesCallbackAndIsLargeEnough() {
        var increments = 0
        setContent(VisitUiState(shopId = "shop-1"), onIncrement = { increments++ })
        val node = composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.visit_pedestrian_increment_desc))

        node.assertWidthIsAtLeast(64.dp).assertHeightIsAtLeast(64.dp)
        node.performClick()
        node.performClick()

        assertEquals(2, increments)
    }

    @Test
    fun decrementButtonInvokesCallbackAndKeepsMinimumTouchTarget() {
        var decrements = 0
        setContent(VisitUiState(shopId = "shop-1"), onDecrement = { decrements++ })
        val node = composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.visit_pedestrian_decrement_desc))

        node.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        node.performClick()

        assertEquals(1, decrements)
    }

    @Test
    fun counterContentDescriptionAnnouncesUpdatedTotal() {
        var state by mutableStateOf(VisitUiState(shopId = "shop-1", pedestrianCount = 1))
        composeTestRule.setContent {
            VidrierAppTheme {
                LogVisitContent(
                    uiState = state,
                    onBackClick = {},
                    onNotesChange = {},
                    onAddressChange = {},
                    onIncrementPedestrians = { state = state.copy(pedestrianCount = state.pedestrianCount + 1) },
                    onDecrementPedestrians = {},
                    onSaveClick = {}
                )
            }
        }
        val one = context.resources.getQuantityString(R.plurals.visit_pedestrian_total_desc, 1, 1)
        val two = context.resources.getQuantityString(R.plurals.visit_pedestrian_total_desc, 2, 2)

        composeTestRule.onNodeWithContentDescription(one).assertContentDescriptionEquals(one)
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.visit_pedestrian_increment_desc))
            .performClick()

        composeTestRule.onNodeWithContentDescription(two).assertContentDescriptionEquals(two)
    }

    @Test
    fun pastVisitsAreListedForAKnownShop() {
        val past = listOf(
            Visit(id = "v1", shopId = "shop-1", timestamp = 1_700_000_000_000L, notes = "esquina concurrida", pedestrianCount = 7)
        )
        setContent(VisitUiState(shopId = "shop-1", pastVisits = past))

        composeTestRule.onNodeWithText(context.getString(R.string.visit_list_title)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("esquina concurrida").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(formatVisitDate(1_700_000_000_000L)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pastVisitsEmptyMessageIsShownForAKnownShopWithoutVisits() {
        setContent(VisitUiState(shopId = "shop-1"))

        composeTestRule.onNodeWithText(context.getString(R.string.visit_no_entries)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pastVisitsSectionIsHiddenWithoutShop() {
        setContent(VisitUiState())

        composeTestRule.onAllNodesWithText(context.getString(R.string.visit_list_title)).assertCountEquals(0)
    }
}
