package com.vidrierapp.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vidrierapp.R
import com.vidrierapp.presentation.theme.minTouchTarget

@Composable
fun OfflineBanner(
    onVisitsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.offline_banner_desc)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.offline_banner_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onVisitsClick,
                modifier = Modifier.minTouchTarget()
            ) {
                Text(text = stringResource(R.string.offline_banner_action))
            }
        }
    }
}

@Composable
fun OfflineBannerSlot(
    visible: Boolean,
    onVisitsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible) {
        OfflineBanner(onVisitsClick = onVisitsClick, modifier = modifier)
    }
}
