package com.vidrierapp.presentation.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vidrierapp.R
import com.vidrierapp.domain.location.Neighborhood
import com.vidrierapp.presentation.theme.minTouchTarget

@Composable
fun LocationFallbackBanner(
    isPermissionDenied: Boolean,
    isPicking: Boolean,
    query: String,
    suggestions: List<Neighborhood>,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (Neighborhood) -> Unit,
    onGrantClick: () -> Unit,
    onPickOnMapClick: () -> Unit,
    onCancelPickClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fallbackDescription = stringResource(R.string.map_location_fallback_desc)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = fallbackDescription },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    if (isPermissionDenied) {
                        R.string.map_location_permission_denied
                    } else {
                        R.string.map_location_unavailable
                    }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            if (isPicking) {
                Text(
                    text = stringResource(R.string.map_location_pick_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = onCancelPickClick,
                    modifier = Modifier.minTouchTarget()
                ) {
                    Text(text = stringResource(R.string.map_location_pick_cancel))
                }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(text = stringResource(R.string.map_search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.action_search)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.minTouchTarget()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.map_location_search_clear)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                suggestions.forEach { neighborhood ->
                    Text(
                        text = neighborhood.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minTouchTarget()
                            .clickable { onSuggestionClick(neighborhood) }
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPickOnMapClick,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Text(text = stringResource(R.string.map_location_pick_on_map))
                    }
                    if (isPermissionDenied) {
                        Button(
                            onClick = onGrantClick,
                            modifier = Modifier.minTouchTarget()
                        ) {
                            Text(text = stringResource(R.string.map_location_grant))
                        }
                    }
                }
            }
        }
    }
}
