package com.vidrierapp.presentation.detail

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vidrierapp.R
import com.vidrierapp.domain.model.ConsumptionFrequency
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.TimeSlot
import com.vidrierapp.domain.model.Weekday

private val FREQUENCIES_SHOWN = listOf(
    ConsumptionFrequency.DIARIA,
    ConsumptionFrequency.SEMANAL,
    ConsumptionFrequency.OCASIONAL
)

private const val PERCENT_TO_FRACTION = 100f

@Composable
internal fun ShopHeader(shop: Shop) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = shop.address, style = MaterialTheme.typography.headlineSmall)
        Text(text = shop.neighborhood, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.detail_surface, shop.surfaceAreaM2),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.detail_price, shop.rentalPrice),
            style = MaterialTheme.typography.bodyLarge
        )
        if (shop.contactAgency.isNotBlank()) {
            Text(
                text = stringResource(R.string.detail_contact_agency, shop.contactAgency),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
internal fun SampleSummary(shop: Shop, showVotes: Boolean) {
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    val parts = buildList {
        if (showVotes) add(stringResource(R.string.detail_sample_votes, shop.totalVotes))
        shop.lastVoteAt?.let { lastVoteAt ->
            val relative = DateUtils.getRelativeTimeSpanString(
                lastVoteAt,
                now,
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
            add(stringResource(R.string.detail_sample_last_vote, relative))
        }
        shop.firstVoteAt?.let { firstVoteAt ->
            val month = DateUtils.formatDateTime(
                context,
                firstVoteAt,
                DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_NO_MONTH_DAY or
                    DateUtils.FORMAT_SHOW_YEAR or
                    DateUtils.FORMAT_ABBREV_MONTH
            )
            add(stringResource(R.string.detail_sample_active_since, month))
        }
    }
    if (parts.isNotEmpty()) {
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
internal fun InfoCard(title: String, message: String? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (message != null) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun ShopMetrics(shop: Shop) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CategoryBreakdown(shop)
        FrequencySection(shop)
        WeekdaySection(shop)
        TimeSlotSection(shop)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun BarRow(
    label: String,
    valueText: String,
    fraction: Float,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(text = valueText, style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CategoryBreakdown(shop: Shop) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.detail_votes_breakdown))
        shop.voteBreakdown.forEach { vote ->
            BarRow(
                label = vote.category.name,
                valueText = stringResource(R.string.detail_votes_count, vote.count, vote.percentage),
                fraction = vote.percentage / PERCENT_TO_FRACTION,
                description = stringResource(
                    R.string.detail_category_bar_desc,
                    vote.category.name,
                    vote.count,
                    vote.percentage
                )
            )
        }
    }
}

@Composable
private fun FrequencySection(shop: Shop) {
    val total = shop.frequencyCounts.filterKeys { it in FREQUENCIES_SHOWN }.values.sum()
    if (total == 0) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.detail_section_frequency))
        FREQUENCIES_SHOWN.forEach { frequency ->
            val count = shop.frequencyCounts[frequency] ?: 0
            val text = when (frequency) {
                ConsumptionFrequency.DIARIA -> stringResource(R.string.detail_freq_daily, count, total)
                ConsumptionFrequency.SEMANAL -> stringResource(R.string.detail_freq_weekly, count, total)
                else -> stringResource(R.string.detail_freq_occasional, count, total)
            }
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun WeekdaySection(shop: Shop) {
    DistributionSection(
        title = stringResource(R.string.detail_section_weekday),
        entries = Weekday.entries.map { day ->
            stringResource(weekdayLabel(day)) to (shop.votesByWeekday[day] ?: 0)
        }
    )
}

@Composable
private fun TimeSlotSection(shop: Shop) {
    DistributionSection(
        title = stringResource(R.string.detail_section_time_slot),
        entries = TimeSlot.entries.map { slot ->
            stringResource(timeSlotLabel(slot)) to (shop.votesByTimeSlot[slot] ?: 0)
        }
    )
}

@Composable
private fun DistributionSection(title: String, entries: List<Pair<String, Int>>) {
    val maxCount = entries.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title)
        entries.forEach { (label, count) ->
            BarRow(
                label = label,
                valueText = count.toString(),
                fraction = count.toFloat() / maxCount,
                description = stringResource(R.string.detail_distribution_bar_desc, label, count)
            )
        }
    }
}

private fun weekdayLabel(day: Weekday): Int = when (day) {
    Weekday.MONDAY -> R.string.weekday_monday
    Weekday.TUESDAY -> R.string.weekday_tuesday
    Weekday.WEDNESDAY -> R.string.weekday_wednesday
    Weekday.THURSDAY -> R.string.weekday_thursday
    Weekday.FRIDAY -> R.string.weekday_friday
    Weekday.SATURDAY -> R.string.weekday_saturday
    Weekday.SUNDAY -> R.string.weekday_sunday
}

private fun timeSlotLabel(slot: TimeSlot): Int = when (slot) {
    TimeSlot.MORNING -> R.string.time_slot_morning
    TimeSlot.AFTERNOON -> R.string.time_slot_afternoon
    TimeSlot.NIGHT -> R.string.time_slot_night
}
