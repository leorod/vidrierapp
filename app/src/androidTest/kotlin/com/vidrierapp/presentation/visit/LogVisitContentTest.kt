package com.vidrierapp.presentation.visit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        onSave: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                LogVisitContent(
                    uiState = state,
                    onBackClick = {},
                    onNotesChange = onNotes,
                    onAddressChange = onAddress,
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
}
