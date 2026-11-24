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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidrierapp.R
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
    onReportRentedClick: () -> Unit = {},
    viewModel: ShopDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(shopId) {
        viewModel.loadShop(shopId)
    }

    ShopDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onLogVisitClick = onLogVisitClick,
        onReportRentedClick = onReportRentedClick,
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
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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

            is ShopDetailUiState.Error -> CenteredColumn(Modifier.padding(innerPadding)) {
                Text(
                    text = stringResource(R.string.status_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.status_error_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)
                ) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }

            is ShopDetailUiState.Success -> ShopDetailBody(
                shop = uiState.shop,
                hasActivePass = uiState.hasActivePass,
                trialAvailable = uiState.trialAvailable,
                onStartTrialClick = onStartTrialClick,
                onPurchasePassClick = onPurchasePassClick,
                onLogVisitClick = onLogVisitClick,
                onReportRentedClick = onReportRentedClick,
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
    onReportRentedClick: () -> Unit,
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
        TextButton(
            onClick = onReportRentedClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(min = MIN_TOUCH_TARGET)
        ) {
            Text(text = stringResource(R.string.detail_btn_report_rented))
        }
    }
}
