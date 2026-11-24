package com.vidrierapp.presentation.visit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidrierapp.R
import com.vidrierapp.presentation.common.AppViewModelProvider

@Composable
fun LogVisitScreen(
    shopId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(shopId) {
        viewModel.initForShop(shopId)
    }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onBackClick()
    }

    LogVisitContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNotesChange = viewModel::updateNotes,
        onAddressChange = viewModel::updateManualAddress,
        onSaveClick = viewModel::saveVisit,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogVisitContent(
    uiState: VisitUiState,
    onBackClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.visit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.requiresManualAddress) {
                OutlinedTextField(
                    value = uiState.manualAddress,
                    onValueChange = onAddressChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    label = { Text(text = stringResource(R.string.visit_address_label)) },
                    placeholder = { Text(text = stringResource(R.string.visit_address_hint)) },
                    isError = uiState.showAddressError,
                    supportingText = if (uiState.showAddressError) {
                        { Text(text = stringResource(R.string.visit_address_required)) }
                    } else {
                        null
                    },
                    singleLine = true
                )
            } else {
                Text(
                    text = stringResource(R.string.visit_shop_target, uiState.shopId),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                label = { Text(text = stringResource(R.string.visit_notes_label)) },
                placeholder = { Text(text = stringResource(R.string.visit_notes_hint)) }
            )
            Text(
                text = stringResource(R.string.visit_privacy_notice),
                style = MaterialTheme.typography.bodySmall
            )
            if (uiState.saveFailed) {
                Text(
                    text = stringResource(R.string.visit_save_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(
                        if (uiState.isSaving) R.string.visit_saving else R.string.visit_btn_submit
                    )
                )
            }
        }
    }
}
