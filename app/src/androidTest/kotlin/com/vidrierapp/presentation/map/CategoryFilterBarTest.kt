package com.vidrierapp.presentation.map

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.vidrierapp.domain.model.Category
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryFilterBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        Category("bakery", "Panaderia"),
        Category("cafe", "Cafeteria")
    )

    private fun setContent(selected: String?, onSelected: (String?) -> Unit = {}) {
        composeTestRule.setContent {
            VidrierAppTheme {
                CategoryFilterBar(
                    categories = categories,
                    selectedCategoryId = selected,
                    onCategorySelected = onSelected
                )
            }
        }
    }

    @Test
    fun allChipIsSelectedWhenNoFilter() {
        setContent(selected = null)

        composeTestRule.onNodeWithText("Todos").assertIsSelected()
        composeTestRule.onNodeWithText("Cafeteria").assertIsNotSelected()
    }

    @Test
    fun selectedCategoryChipIsSelected() {
        setContent(selected = "cafe")

        composeTestRule.onNodeWithText("Cafeteria").assertIsSelected()
        composeTestRule.onNodeWithText("Todos").assertIsNotSelected()
    }

    @Test
    fun tappingCategoryInvokesCallbackWithId() {
        var received: String? = "unset"
        setContent(selected = null) { received = it }

        composeTestRule.onNodeWithText("Panaderia").performClick()

        assertEquals("bakery", received)
    }

    @Test
    fun tappingAllInvokesCallbackWithNull() {
        var received: String? = "unset"
        setContent(selected = "cafe") { received = it }

        composeTestRule.onNodeWithText("Todos").performClick()

        assertEquals(null, received)
    }

    @Test
    fun chipsMeetMinimumTouchTarget() {
        setContent(selected = null)

        composeTestRule.onNodeWithText("Todos").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithText("Cafeteria").assertHeightIsAtLeast(48.dp)
    }
}
