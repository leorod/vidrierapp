package com.vidrierapp.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vidrierapp.R
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.presentation.theme.minTouchTarget


internal fun launchContactIntent(context: Context, data: ContactIntentData?): Boolean {
    if (data == null) return false
    val intent = Intent(data.action, Uri.parse(data.uri))
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

@Composable
internal fun ContactActions(
    shop: Shop,
    onCallClick: (ContactIntentData?) -> Unit,
    onWhatsAppClick: (ContactIntentData?) -> Unit,
    onEmailClick: (ContactIntentData?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhone = shop.contactPhone.isNotBlank()
    val hasEmail = shop.contactEmail.isNotBlank()
    if (!hasPhone && !hasEmail) return
    val whatsAppMessage = stringResource(R.string.detail_whatsapp_message, shop.address)
    val emailSubject = stringResource(R.string.detail_email_subject, shop.address)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.detail_section_contact),
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasPhone) {
                ContactButton(
                    label = stringResource(R.string.detail_btn_call),
                    description = stringResource(R.string.detail_btn_call_desc),
                    onClick = { onCallClick(ContactIntents.dial(shop.contactPhone)) }
                )
                ContactButton(
                    label = stringResource(R.string.detail_btn_whatsapp),
                    description = stringResource(R.string.detail_btn_whatsapp_desc),
                    onClick = {
                        onWhatsAppClick(ContactIntents.whatsApp(shop.contactPhone, whatsAppMessage))
                    }
                )
            }
            if (hasEmail) {
                ContactButton(
                    label = stringResource(R.string.detail_btn_email),
                    description = stringResource(R.string.detail_btn_email_desc),
                    onClick = { onEmailClick(ContactIntents.email(shop.contactEmail, emailSubject)) }
                )
            }
        }
    }
}

@Composable
private fun ContactButton(label: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .minTouchTarget()
            .semantics { contentDescription = description }
    ) {
        Text(text = label)
    }
}
