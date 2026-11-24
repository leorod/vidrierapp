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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidrierapp.R
import com.vidrierapp.domain.model.Visit
import com.vidrierapp.presentation.common.AppViewModelProvider
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVisitsHubScreen(
    onNewVisitClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyVisitsHubViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
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
                    onClick = viewModel::loadVisits,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(text = stringResource(R.string.action_retry))
                }
            }

            is MyVisitsHubUiState.Success -> if (state.visits.isEmpty()) {
                CenteredContent(innerPadding) {
                    Text(
                        text = stringResource(R.string.hub_empty),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.visits, key = { it.id }) { visit ->
                        VisitCard(visit = visit)
                    }
                }
            }
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
private fun VisitCard(visit: Visit, modifier: Modifier = Modifier) {
    val formattedDate = DateFormat
        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(visit.timestamp))

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = formattedDate, style = MaterialTheme.typography.titleMedium)
            if (visit.shopId.isNotBlank()) {
                Text(
                    text = stringResource(R.string.visit_shop_target, visit.shopId),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = stringResource(R.string.visit_pedestrian_count, visit.pedestrianCount),
                style = MaterialTheme.typography.bodyMedium
            )
            if (visit.notes.isNotBlank()) {
                Text(text = visit.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
