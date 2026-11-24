package com.vidrierapp.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vidrierapp.R
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.common.AppViewModelProvider
import kotlinx.coroutines.launch

private const val HUE_RANGE = 360
private const val NEUTRAL_MARKER_HUE = 210f
private const val NEUTRAL_MARKER_ALPHA = 0.6f
private const val SINGLE_SHOP_ZOOM = 14f
private const val DEFAULT_ZOOM = 13f
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

    when (val state = uiState) {
        MapUiState.Loading -> CenteredMessage(modifier) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.status_loading),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        is MapUiState.Error -> CenteredMessage(modifier) {
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
                onClick = viewModel::retry,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(text = stringResource(R.string.action_retry))
            }
        }

        is MapUiState.Success -> ShopsMap(
            shops = state.shops,
            onShopSelected = viewModel::selectShop,
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
    onShopSelected: (Shop) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }

    LaunchedEffect(shops) {
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

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
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
                        false
                    }
                )
            }
        }
        if (shops.isEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = stringResource(R.string.map_no_shops_found),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
