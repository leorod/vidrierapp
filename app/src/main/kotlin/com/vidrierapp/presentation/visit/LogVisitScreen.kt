package com.vidrierapp.presentation.visit

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cameraUnavailableMessage = stringResource(R.string.visit_camera_unavailable)
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onPhotoCaptureResult(success)
    }
    val launchCamera: () -> Unit = {
        val captureUri = viewModel.startPhotoCapture()
        try {
            takePicture.launch(Uri.parse(captureUri))
        } catch (_: ActivityNotFoundException) {
            viewModel.onPhotoCaptureResult(false)
            scope.launch { snackbarHostState.showSnackbar(cameraUnavailableMessage) }
        }
    }

    LogVisitContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onAddPhotoClick = launchCamera,
        onRemovePhoto = viewModel::removePhoto,
        snackbarHostState = snackbarHostState,
        onNotesChange = viewModel::updateNotes,
        onAddressChange = viewModel::updateManualAddress,
        onIncrementPedestrians = viewModel::incrementPedestrians,
        onDecrementPedestrians = viewModel::decrementPedestrians,
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
    onIncrementPedestrians: () -> Unit,
    onDecrementPedestrians: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddPhotoClick: () -> Unit = {},
    onRemovePhoto: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val haptics = LocalHapticFeedback.current
    val takePhotoDescription = stringResource(R.string.visit_btn_take_photo_desc)
    val totalDescription = pluralStringResource(
        R.plurals.visit_pedestrian_total_desc,
        uiState.pedestrianCount,
        uiState.pedestrianCount
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            Text(
                text = stringResource(R.string.visit_pedestrian_label),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDecrementPedestrians()
                    },
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.visit_pedestrian_decrement_desc)
                    )
                }
                Text(
                    text = uiState.pedestrianCount.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = totalDescription
                            liveRegion = LiveRegionMode.Polite
                        },
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onIncrementPedestrians()
                    },
                    modifier = Modifier.size(88.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.visit_pedestrian_increment_desc),
                        modifier = Modifier.size(48.dp)
                    )
                }
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
                text = stringResource(R.string.visit_photos_label),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(
                onClick = onAddPhotoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = takePhotoDescription
                    }
            ) {
                Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null)
                Text(
                    text = stringResource(R.string.visit_btn_take_photo),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (uiState.photoUris.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        R.plurals.visit_photos_count,
                        uiState.photoUris.size,
                        uiState.photoUris.size
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                PhotoThumbnailsRow(
                    photoPaths = uiState.photoUris,
                    modifier = Modifier.fillMaxWidth(),
                    onRemovePhoto = onRemovePhoto
                )
            }
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
