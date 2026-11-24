package com.vidrierapp.presentation.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.location.NeighborhoodCatalog
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocationFallbackBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        isPermissionDenied: Boolean = true,
        isPicking: Boolean = false,
        query: String = "",
        onGrant: () -> Unit = {},
        onPick: () -> Unit = {},
        onSuggestion: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                LocationFallbackBanner(
                    isPermissionDenied = isPermissionDenied,
                    isPicking = isPicking,
                    query = query,
                    suggestions = NeighborhoodCatalog.search(query),
                    onQueryChange = {},
                    onSuggestionClick = { onSuggestion(it.name) },
                    onGrantClick = onGrant,
                    onPickOnMapClick = onPick,
                    onCancelPickClick = {}
                )
            }
        }
    }

    @Test
    fun deniedShowsMessageAndActions() {
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.map_location_permission_denied))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_location_pick_on_map))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_location_grant))
            .assertIsDisplayed()
    }

    @Test
    fun unavailableHidesGrantButton() {
        setContent(isPermissionDenied = false)

        composeTestRule.onNodeWithText(context.getString(R.string.map_location_unavailable))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_location_grant))
            .assertDoesNotExist()
    }

    @Test
    fun grantAndPickCallbacksFire() {
        var granted = 0
        var picked = 0
        setContent(onGrant = { granted++ }, onPick = { picked++ })

        composeTestRule.onNodeWithText(context.getString(R.string.map_location_grant)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.map_location_pick_on_map)).performClick()

        assertEquals(1, granted)
        assertEquals(1, picked)
    }

    @Test
    fun suggestionClickReportsNeighborhood() {
        var selected = ""
        setContent(query = "Matad", onSuggestion = { selected = it })

        composeTestRule.onNodeWithText("Mataderos").assertIsDisplayed().performClick()

        assertEquals("Mataderos", selected)
    }

    @Test
    fun pickingShowsHintInsteadOfSearch() {
        setContent(isPicking = true)

        composeTestRule.onNodeWithText(context.getString(R.string.map_location_pick_hint))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.map_location_pick_on_map))
            .assertDoesNotExist()
    }
}
