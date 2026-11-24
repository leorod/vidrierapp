package com.vidrierapp.presentation.detail

import java.net.URLEncoder

data class ContactIntentData(val action: String, val uri: String)

object ContactIntents {

    const val ACTION_DIAL = "android.intent.action.DIAL"
    const val ACTION_VIEW = "android.intent.action.VIEW"
    const val ACTION_SENDTO = "android.intent.action.SENDTO"

    private const val WHATSAPP_SEND_URL = "https://api.whatsapp.com/send"

    fun normalizePhone(raw: String): String {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }

    fun dial(phone: String): ContactIntentData? {
        val normalized = normalizePhone(phone)
        if (normalized.none { it.isDigit() }) return null
        return ContactIntentData(ACTION_DIAL, "tel:$normalized")
    }

    fun whatsApp(phone: String, message: String): ContactIntentData? {
        val digits = normalizePhone(phone).filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return ContactIntentData(
            ACTION_VIEW,
            "$WHATSAPP_SEND_URL?phone=$digits&text=${encode(message)}"
        )
    }

    fun email(address: String, subject: String): ContactIntentData? {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return null
        return ContactIntentData(ACTION_SENDTO, "mailto:$trimmed?subject=${encode(subject)}")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
