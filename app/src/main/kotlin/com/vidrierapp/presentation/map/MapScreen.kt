package com.vidrierapp.presentation.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vidrierapp.R
import com.vidrierapp.presentation.common.ErrorState
import com.vidrierapp.domain.location.Neighborhood
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.GeoPoint
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.common.AppViewModelProvider
import kotlinx.coroutines.launch

private const val HUE_RANGE = 360
private const val NEUTRAL_MARKER_HUE = 210f
private const val NEUTRAL_MARKER_ALPHA = 0.6f
private const val SINGLE_SHOP_ZOOM = 14f
private const val DEFAULT_ZOOM = 13f
private const val USER_LOCATION_ZOOM = 15f
private const val BOUNDS_PADDING_PX = 120
private val DEFAULT_LOCATION = LatLng(-34.65, -58.50)

internal fun categoryMarkerHue(categoryId: String): Float =
    Math.floorMod(categoryId.hashCode(), HUE_RANGE).toFloat()

@Composable
fun MapScreen(
    onShopClick: (shopId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> viewModel.onPermissionResult(results.values.any { it }) }
    val requestLocationPermission = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(locationState.permission) {
        if (locationState.permission == LocationPermission.Unknown) {
            val granted = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (granted) viewModel.onPermissionResult(true) else requestLocationPermission()
        }
    }

    when (val state = uiState) {
        MapUiState.Loading -> CenteredMessage(modifier) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.status_loading),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        is MapUiState.Error -> ErrorState(
            isOffline = state.isOffline,
            onRetry = viewModel::retry,
            modifier = modifier
        )

        is MapUiState.Success -> ShopsMap(
            shops = state.shops,
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryFilter,
            onCategorySelected = viewModel::selectCategory,
            selectedShop = state.selectedShop,
            onShopSelected = viewModel::selectShop,
            onShopDismissed = { viewModel.selectShop(null) },
            onShopClick = onShopClick,
            locationState = locationState,
            onMyLocationClick = {
                if (locationState.isGranted) viewModel.recenterOnUser() else requestLocationPermission()
            },
            onGrantClick = requestLocationPermission,
            onQueryChange = viewModel::onSearchQueryChange,
            onNeighborhoodSelected = viewModel::onNeighborhoodSelected,
            onPickOnMapClick = viewModel::startManualPick,
            onCancelPickClick = viewModel::cancelManualPick,
            onManualLocationSelected = viewModel::onManualLocationSelected,
            modifier = modifier
        )
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
private fun ShopsMap(
    shops: List<Shop>,
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    selectedShop: Shop?,
    onShopSelected: (Shop) -> Unit,
    onShopDismissed: () -> Unit,
    onShopClick: (shopId: String) -> Unit,
    locationState: LocationUiState,
    onMyLocationClick: () -> Unit,
    onGrantClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onNeighborhoodSelected: (Neighborhood) -> Unit,
    onPickOnMapClick: () -> Unit,
    onCancelPickClick: () -> Unit,
    onManualLocationSelected: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLocationState by rememberUpdatedState(locationState)
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }

    LaunchedEffect(shops) {
        if (currentLocationState.centerRequest != null) return@LaunchedEffect
        when {
            shops.isEmpty() -> cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
            )

            shops.size == 1 -> cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(shops.first().latitude, shops.first().longitude),
                    SINGLE_SHOP_ZOOM
                )
            )

            else -> {
                val bounds = LatLngBounds.builder()
                shops.forEach { bounds.include(LatLng(it.latitude, it.longitude)) }
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngBounds(bounds.build(), BOUNDS_PADDING_PX)
                )
            }
        }
    }

    LaunchedEffect(locationState.centerRequest?.id) {
        locationState.centerRequest?.let { request ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(request.point.latitude, request.point.longitude),
                    USER_LOCATION_ZOOM
                )
            )
        }
    }

    var lastSelectedShop by remember { mutableStateOf(selectedShop) }
    if (selectedShop != null && selectedShop != lastSelectedShop) {
        lastSelectedShop = selectedShop
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationState.isGranted),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false),
            onMapClick = { latLng ->
                if (locationState.isPickingManually) {
                    onManualLocationSelected(GeoPoint(latLng.latitude, latLng.longitude))
                } else {
                    onShopDismissed()
                }
            }
        ) {
            locationState.manualLocation?.let { manual ->
                Marker(
                    state = MarkerState(position = LatLng(manual.latitude, manual.longitude)),
                    title = stringResource(R.string.map_location_manual_marker),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
            shops.forEach { shop ->
                val position = LatLng(shop.latitude, shop.longitude)
                val title = if (shop.isSampleSufficient) {
                    shop.topCategory?.name.orEmpty()
                } else {
                    stringResource(
                        R.string.map_marker_collecting,
                        shop.totalVotes,
                        MIN_VOTES_FOR_METRICS
                    )
                }
                val hue = if (shop.isSampleSufficient) {
                    categoryMarkerHue(shop.topCategory?.id.orEmpty())
                } else {
                    NEUTRAL_MARKER_HUE
                }
                Marker(
                    state = MarkerState(position = position),
                    title = title,
                    snippet = shop.address,
                    icon = BitmapDescriptorFactory.defaultMarker(hue),
                    alpha = if (shop.isSampleSufficient) 1f else NEUTRAL_MARKER_ALPHA,
                    onClick = {
                        onShopSelected(shop)
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLng(position))
                        }
                        true
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = selectedShop != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            lastSelectedShop?.let { previewShop ->
                ShopPreviewCard(
                    shop = previewShop,
                    onViewDetailClick = { onShopClick(previewShop.id) },
                    onDismiss = onShopDismissed,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryFilterBar(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )
            if (locationState.showFallback) {
                LocationFallbackBanner(
                    isPermissionDenied = locationState.permission == LocationPermission.Denied,
                    isPicking = locationState.isPickingManually,
                    query = locationState.searchQuery,
                    suggestions = locationState.suggestions,
                    onQueryChange = onQueryChange,
                    onSuggestionClick = onNeighborhoodSelected,
                    onGrantClick = onGrantClick,
                    onPickOnMapClick = onPickOnMapClick,
                    onCancelPickClick = onCancelPickClick,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (shops.isEmpty()) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.map_no_shops_found),
                            textAlign = TextAlign.Center
                        )
                        if (selectedCategoryId != null) {
                            Button(
                                onClick = { onCategorySelected(null) },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(text = stringResource(R.string.map_filter_reset))
                            }
                        }
                    }
                }
            }
        }
        if (selectedShop == null) {
            FloatingActionButton(
                onClick = onMyLocationClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = stringResource(R.string.map_btn_my_location_desc)
                )
            }
        }
    }
}
