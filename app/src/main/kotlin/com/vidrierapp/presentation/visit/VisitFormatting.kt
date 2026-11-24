package com.vidrierapp.presentation.visit

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatVisitDate(timestamp: Long, locale: Locale = Locale.getDefault()): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        .format(Date(timestamp))
