package com.vidrierapp.presentation.map

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vidrierapp.R
import com.vidrierapp.domain.model.MIN_VOTES_FOR_METRICS
import com.vidrierapp.domain.model.Shop
import kotlin.math.roundToInt

const val SHOP_PREVIEW_CARD_TAG = "shop_preview_card"
private const val DISMISS_DRAG_THRESHOLD_DP = 72

internal fun shouldDismissOnDrag(dragDistancePx: Float, thresholdPx: Float): Boolean =
    dragDistancePx >= thresholdPx

@Composable
fun ShopPreviewCard(
    shop: Shop,
    onViewDetailClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetPx by remember(shop.id) { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(LocalDensity.current) { DISMISS_DRAG_THRESHOLD_DP.dp.toPx() }

    ElevatedCard(
        onClick = onViewDetailClick,
        modifier = modifier
            .testTag(SHOP_PREVIEW_CARD_TAG)
            .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
                },
                onDragStopped = {
                    if (shouldDismissOnDrag(dragOffsetPx, dismissThresholdPx)) {
                        onDismiss()
                    }
                    dragOffsetPx = 0f
                }
            )
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    Text(
                        text = shop.address,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = shop.neighborhood,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }
            Column(modifier = Modifier.padding(top = 8.dp, end = 8.dp)) {
                if (shop.isSampleSufficient) {
                    Text(
                        text = stringResource(R.string.map_card_top_rubro) +
                            " " + shop.topCategory?.name.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.map_marker_collecting,
                            shop.totalVotes,
                            MIN_VOTES_FOR_METRICS
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.map_card_total_votes, shop.totalVotes),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Button(
                    onClick = onViewDetailClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(top = 12.dp)
                ) {
                    Text(text = stringResource(R.string.map_card_view_detail))
                }
            }
        }
    }
}
