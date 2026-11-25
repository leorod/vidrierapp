package com.vidrierapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidrierapp.R
import com.vidrierapp.presentation.common.ErrorState
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.common.AppViewModelProvider

private val MIN_TOUCH_TARGET = 48.dp

@Composable
fun ShopDetailScreen(
    shopId: String,
    onBackClick: () -> Unit,
    onLogVisitClick: (shopId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(shopId) {
        viewModel.loadShop(shopId)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val noAppMessage = stringResource(R.string.detail_contact_no_app)
    val launch: (ContactIntentData?) -> Unit = { data ->
        if (!launchContactIntent(context, data)) {
            scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
        }
    }

    val reportState by viewModel.reportState.collectAsState()
    ReportRentedFeedbackEffect(
        feedback = viewModel.feedback,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::confirmReportRented
    )

    ShopDetailContent(
        uiState = uiState,
        reportState = reportState,
        snackbarHostState = snackbarHostState,
        onCallClick = launch,
        onWhatsAppClick = launch,
        onEmailClick = launch,
        onBackClick = onBackClick,
        onLogVisitClick = onLogVisitClick,
        onReportRentedClick = viewModel::onReportRentedClick,
        onReportConfirm = viewModel::confirmReportRented,
        onReportDismiss = viewModel::dismissReportDialog,
        onRetry = viewModel::retry,
        onStartTrialClick = viewModel::startTrial,
        onPurchasePassClick = viewModel::purchasePass,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailContent(
    uiState: ShopDetailUiState,
    onBackClick: () -> Unit,
    onLogVisitClick: (shopId: String) -> Unit,
    onReportRentedClick: () -> Unit,
    onRetry: () -> Unit,
    onStartTrialClick: () -> Unit,
    onPurchasePassClick: () -> Unit,
    modifier: Modifier = Modifier,
    reportState: ReportRentedUiState = ReportRentedUiState(),
    onReportConfirm: () -> Unit = {},
    onReportDismiss: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onCallClick: (ContactIntentData?) -> Unit = {},
    onWhatsAppClick: (ContactIntentData?) -> Unit = {},
    onEmailClick: (ContactIntentData?) -> Unit = {}
) {
    if (reportState.isDialogVisible) {
        ReportRentedDialog(onConfirm = onReportConfirm, onDismiss = onReportDismiss)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            ShopDetailUiState.Loading -> CenteredColumn(Modifier.padding(innerPadding)) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.status_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            is ShopDetailUiState.Error -> ErrorState(
                isOffline = uiState.isOffline,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding)
            )

            is ShopDetailUiState.Success -> ShopDetailBody(
                shop = uiState.shop,
                hasActivePass = uiState.hasActivePass,
                trialAvailable = uiState.trialAvailable,
                onStartTrialClick = onStartTrialClick,
                onPurchasePassClick = onPurchasePassClick,
                onLogVisitClick = onLogVisitClick,
                reportState = reportState,
                onReportRentedClick = onReportRentedClick,
                onCallClick = onCallClick,
                onWhatsAppClick = onWhatsAppClick,
                onEmailClick = onEmailClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ShopDetailBody(
    shop: Shop,
    hasActivePass: Boolean,
    trialAvailable: Boolean,
    onStartTrialClick: () -> Unit,
    onPurchasePassClick: () -> Unit,
    onLogVisitClick: (shopId: String) -> Unit,
    reportState: ReportRentedUiState,
    onReportRentedClick: () -> Unit,
    onCallClick: (ContactIntentData?) -> Unit,
    onWhatsAppClick: (ContactIntentData?) -> Unit,
    onEmailClick: (ContactIntentData?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShopHeader(shop)
        SampleSummary(shop, showVotes = hasActivePass)
        ContactActions(
            shop = shop,
            onCallClick = onCallClick,
            onWhatsAppClick = onWhatsAppClick,
            onEmailClick = onEmailClick
        )
        when {
            !hasActivePass -> PaywallCard(
                trialAvailable = trialAvailable,
                onStartTrialClick = onStartTrialClick,
                onPurchasePassClick = onPurchasePassClick
            )

            !shop.isSampleSufficient -> InfoCard(
                title = stringResource(
                    R.string.detail_sample_insufficient,
                    shop.totalVotes,
                    MIN_VOTES_FOR_METRICS
                )
            )

            else -> ShopMetrics(shop)
        }
        val logVisitDescription = stringResource(R.string.detail_btn_log_visit_desc)
        Button(
            onClick = { onLogVisitClick(shop.id) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET)
                .semantics { contentDescription = logVisitDescription }
        ) {
            Text(text = stringResource(R.string.detail_btn_log_visit))
        }
        ReportRentedButton(
            alreadyReported = reportState.alreadyReported,
            isSubmitting = reportState.isSubmitting,
            onClick = onReportRentedClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
