package com.vidrierapp.presentation.visit

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.presentation.theme.VidrierAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PhotoThumbnailsRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val paths = listOf("/data/none/a.jpg", "/data/none/b.jpg")

    @Test
    fun showsOneThumbnailPerPhoto() {
        composeTestRule.setContent {
            VidrierAppTheme { PhotoThumbnailsRow(photoPaths = paths, onRemovePhoto = {}) }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.visit_photo_thumbnail_desc, 1)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.visit_photo_thumbnail_desc, 2)).assertIsDisplayed()
    }

    @Test
    fun deleteButtonInvokesCallbackWithItsPathAndKeepsTouchTarget() {
        var removed = ""
        composeTestRule.setContent {
            VidrierAppTheme { PhotoThumbnailsRow(photoPaths = paths, onRemovePhoto = { removed = it }) }
        }
        val delete = composeTestRule.onNodeWithContentDescription(context.getString(R.string.visit_photo_delete_desc, 2))

        delete.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        delete.performClick()

        assertEquals(paths[1], removed)
    }

    @Test
    fun readOnlyRowHasNoDeleteButtons() {
        composeTestRule.setContent {
            VidrierAppTheme { PhotoThumbnailsRow(photoPaths = paths) }
        }

        composeTestRule
            .onAllNodesWithContentDescription(context.getString(R.string.visit_photo_delete_desc, 1))
            .assertCountEquals(0)
    }

    @Test
    fun logVisitContentRendersThumbnailsAndForwardsDeleteAndAdd() {
        var removed = ""
        var adds = 0
        composeTestRule.setContent {
            VidrierAppTheme {
                LogVisitContent(
                    uiState = VisitUiState(shopId = "shop-1", photoUris = paths),
                    onBackClick = {},
                    onNotesChange = {},
                    onAddressChange = {},
                    onIncrementPedestrians = {},
                    onDecrementPedestrians = {},
                    onSaveClick = {},
                    onAddPhotoClick = { adds++ },
                    onRemovePhoto = { removed = it }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.visit_btn_take_photo_desc)).performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.visit_photo_delete_desc, 1)).performClick()

        assertEquals(1, adds)
        assertEquals(paths[0], removed)
    }
}
