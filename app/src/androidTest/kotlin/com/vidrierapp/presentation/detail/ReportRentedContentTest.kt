package com.vidrierapp.presentation.detail

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.vidrierapp.R
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.theme.VidrierAppTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReportRentedContentTest {

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
        topCategory = null,
        totalVotes = 20
    )

    private fun setDetail(
        reportState: ReportRentedUiState,
        onReportRentedClick: () -> Unit = {},
        onReportConfirm: () -> Unit = {},
        onReportDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            VidrierAppTheme {
                ShopDetailContent(
                    uiState = ShopDetailUiState.Success(shop, hasActivePass = true),
                    reportState = reportState,
                    onBackClick = {},
                    onLogVisitClick = {},
                    onReportRentedClick = onReportRentedClick,
                    onReportConfirm = onReportConfirm,
                    onReportDismiss = onReportDismiss,
                    onRetry = {},
                    onStartTrialClick = {},
                    onPurchasePassClick = {}
                )
            }
        }
    }

    @Test
    fun reportButtonInvokesCallbackAndMeetsTouchTarget() {
        var clicks = 0
        setDetail(ReportRentedUiState(), onReportRentedClick = { clicks++ })

        val button = composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_report_rented))
        button.performScrollTo().assertHeightIsAtLeast(48.dp).assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun dialogIsHiddenWhenNotVisible() {
        setDetail(ReportRentedUiState(isDialogVisible = false))

        composeTestRule.onNodeWithText(context.getString(R.string.report_rented_dialog_title)).assertDoesNotExist()
    }

    @Test
    fun dialogConfirmInvokesConfirmOnly() {
        var confirms = 0
        var dismisses = 0
        setDetail(
            ReportRentedUiState(isDialogVisible = true),
            onReportConfirm = { confirms++ },
            onReportDismiss = { dismisses++ }
        )

        composeTestRule.onNodeWithText(context.getString(R.string.report_rented_dialog_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.report_rented_dialog_confirm)).performClick()

        assertEquals(1, confirms)
        assertEquals(0, dismisses)
    }

    @Test
    fun dialogCancelInvokesDismissOnly() {
        var confirms = 0
        var dismisses = 0
        setDetail(
            ReportRentedUiState(isDialogVisible = true),
            onReportConfirm = { confirms++ },
            onReportDismiss = { dismisses++ }
        )

        composeTestRule.onNodeWithText(context.getString(R.string.action_cancel)).performClick()

        assertEquals(0, confirms)
        assertEquals(1, dismisses)
    }

    @Test
    fun alreadyReportedDisablesButtonAndShowsLabel() {
        var clicks = 0
        setDetail(ReportRentedUiState(alreadyReported = true), onReportRentedClick = { clicks++ })

        composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_report_rented)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.detail_btn_report_rented_done))
            .performScrollTo()
            .assertIsNotEnabled()

        assertEquals(0, clicks)
    }

    @Composable
    private fun FeedbackHost(
        feedback: kotlinx.coroutines.flow.Flow<ReportRentedFeedback>,
        state: SnackbarHostState,
        onRetry: () -> Unit
    ) {
        VidrierAppTheme {
            ReportRentedFeedbackEffect(feedback = feedback, snackbarHostState = state, onRetry = onRetry)
            SnackbarHost(state)
        }
    }

    @Test
    fun successFeedbackShowsMessage() {
        val state = SnackbarHostState()
        composeTestRule.setContent {
            FeedbackHost(flowOf(ReportRentedFeedback.Success), state) {}
        }

        composeTestRule.onNodeWithText(context.getString(R.string.report_rented_success)).assertIsDisplayed()
    }

    @Test
    fun alreadyReportedFeedbackShowsMessage() {
        val state = SnackbarHostState()
        composeTestRule.setContent {
            FeedbackHost(flowOf(ReportRentedFeedback.AlreadyReported), state) {}
        }

        composeTestRule.onNodeWithText(context.getString(R.string.report_rented_already)).assertIsDisplayed()
    }

    @Test
    fun errorFeedbackShowsMessageAndRetryAction() {
        val state = SnackbarHostState()
        val events = MutableSharedFlow<ReportRentedFeedback>(replay = 1)
        events.tryEmit(ReportRentedFeedback.Failure)
        var retries = 0
        composeTestRule.setContent {
            FeedbackHost(events, state) { retries++ }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.report_rented_error)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.action_retry)).performClick()

        composeTestRule.waitForIdle()
        assertEquals(1, retries)
    }
}
