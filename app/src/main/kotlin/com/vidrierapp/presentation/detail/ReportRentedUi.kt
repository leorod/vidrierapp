package com.vidrierapp.presentation.detail

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vidrierapp.R
import kotlinx.coroutines.flow.Flow

private val MIN_TOUCH_TARGET = 48.dp

@Composable
fun ReportRentedButton(
    alreadyReported: Boolean,
    isSubmitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(
        if (alreadyReported) R.string.detail_btn_report_rented_done_desc
        else R.string.detail_btn_report_rented_desc
    )
    TextButton(
        onClick = onClick,
        enabled = !alreadyReported && !isSubmitting,
        modifier = modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .semantics { contentDescription = description }
    ) {
        Text(
            text = stringResource(
                if (alreadyReported) R.string.detail_btn_report_rented_done
                else R.string.detail_btn_report_rented
            )
        )
    }
}

@Composable
fun ReportRentedDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.report_rented_dialog_title)) },
        text = { Text(text = stringResource(R.string.report_rented_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)
            ) {
                Text(text = stringResource(R.string.report_rented_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)
            ) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ReportRentedFeedbackEffect(
    feedback: Flow<ReportRentedFeedback>,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit
) {
    val successMessage = stringResource(R.string.report_rented_success)
    val failureMessage = stringResource(R.string.report_rented_error)
    val alreadyMessage = stringResource(R.string.report_rented_already)
    val retryLabel = stringResource(R.string.action_retry)
    val currentOnRetry by rememberUpdatedState(onRetry)

    LaunchedEffect(feedback) {
        feedback.collect { event ->
            when (event) {
                ReportRentedFeedback.Success ->
                    snackbarHostState.showSnackbar(successMessage)

                ReportRentedFeedback.AlreadyReported ->
                    snackbarHostState.showSnackbar(alreadyMessage)

                ReportRentedFeedback.Failure -> {
                    val result = snackbarHostState.showSnackbar(
                        message = failureMessage,
                        actionLabel = retryLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) currentOnRetry()
                }
            }
        }
    }
}
