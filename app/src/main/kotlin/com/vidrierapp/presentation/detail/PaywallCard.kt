package com.vidrierapp.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vidrierapp.R

private val MIN_TOUCH_TARGET = 48.dp

@Composable
internal fun PaywallCard(
    trialAvailable: Boolean,
    onStartTrialClick: () -> Unit,
    onPurchasePassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.pass_locked_icon_desc),
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.pass_locked_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.pass_locked_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.pass_simulation_notice),
                style = MaterialTheme.typography.bodySmall
            )
            if (trialAvailable) {
                val trialDescription = stringResource(R.string.pass_btn_start_trial_desc)
                Button(
                    onClick = onStartTrialClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MIN_TOUCH_TARGET)
                        .semantics { contentDescription = trialDescription }
                ) {
                    Text(text = stringResource(R.string.pass_btn_start_trial))
                }
            }
            val passDescription = stringResource(R.string.pass_btn_activate_desc)
            val passModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET)
                .semantics { contentDescription = passDescription }
            if (trialAvailable) {
                OutlinedButton(onClick = onPurchasePassClick, modifier = passModifier) {
                    Text(text = stringResource(R.string.pass_btn_activate))
                }
            } else {
                Button(onClick = onPurchasePassClick, modifier = passModifier) {
                    Text(text = stringResource(R.string.pass_btn_activate))
                }
            }
        }
    }
}
