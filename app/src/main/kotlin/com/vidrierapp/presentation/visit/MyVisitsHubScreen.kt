package com.vidrierapp.presentation.visit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidrierapp.R
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.presentation.common.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVisitsHubScreen(
    onNewVisitClick: () -> Unit,
    onSavedShopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyVisitsHubViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    LifecycleResumeEffect(viewModel) {
        viewModel.loadVisits()
        onPauseOrDispose { }
    }
    MyVisitsHubContent(
        uiState = uiState,
        onNewVisitClick = onNewVisitClick,
        onSavedShopClick = onSavedShopClick,
        onRetryClick = viewModel::loadVisits,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVisitsHubContent(
    uiState: MyVisitsHubUiState,
    onNewVisitClick: () -> Unit,
    onSavedShopClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fabDescription = stringResource(R.string.hub_fab_desc)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.hub_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewVisitClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = fabDescription },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.hub_fab_label)) }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            MyVisitsHubUiState.Loading -> CenteredContent(innerPadding) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.status_loading),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            is MyVisitsHubUiState.Error -> CenteredContent(innerPadding) {
                Text(
                    text = stringResource(R.string.status_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.status_error_message),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }

            is MyVisitsHubUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "visits_header") {
                    SectionTitle(text = stringResource(R.string.hub_visits_section))
                }
                if (state.visits.isEmpty()) {
                    item(key = "visits_empty") {
                        Text(
                            text = stringResource(R.string.hub_empty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(state.visits, key = { "visit_" + it.id }) { visit ->
                        VisitCard(
                            visit = visit,
                            shopName = visit.shopId?.let { state.shopNames[it] }
                        )
                    }
                }
                item(key = "saved_header") {
                    SectionTitle(text = stringResource(R.string.hub_saved_shops_section))
                }
                if (state.savedShops.isEmpty()) {
                    item(key = "saved_empty") {
                        Text(
                            text = stringResource(R.string.hub_saved_shops_empty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(state.savedShops, key = { "saved_" + it.shopId }) { item ->
                        SavedShopCard(item = item, onClick = { onSavedShopClick(item.shopId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun SavedShopCard(item: SavedShopItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.hub_saved_shop_desc, item.address)
    val age = if (item.daysAgo == 0) {
        stringResource(R.string.hub_saved_today)
    } else {
        pluralStringResource(R.plurals.hub_saved_days_ago, item.daysAgo, item.daysAgo)
    }
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = item.address, style = MaterialTheme.typography.titleMedium)
            if (item.neighborhood.isNotBlank()) {
                Text(text = item.neighborhood, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = age, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CenteredContent(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}

@Composable
private fun VisitCard(visit: Visit, shopName: String?, modifier: Modifier = Modifier) {
    val formattedDate = formatVisitDate(visit.timestamp)

    val galleryDescription = stringResource(R.string.hub_photos_gallery_desc)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = formattedDate, style = MaterialTheme.typography.titleMedium)
            if (!visit.shopId.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        R.string.visit_shop_target,
                        shopName ?: stringResource(R.string.hub_visit_shop_unknown)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!visit.manualAddress.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.visit_manual_address_target, visit.manualAddress),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = pluralStringResource(R.plurals.visit_pedestrian_count, visit.pedestrianCount, visit.pedestrianCount),
                style = MaterialTheme.typography.bodyMedium
            )
            if (visit.photoUris.isNotEmpty()) {
                PhotoThumbnailsRow(
                    photoPaths = visit.photoUris,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics { contentDescription = galleryDescription },
                    thumbnailSize = 56.dp
                )
            }
            if (visit.notes.isNotBlank()) {
                Text(text = visit.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
