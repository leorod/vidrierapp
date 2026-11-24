package com.vidrierapp.data.mapper

import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.data.remote.dto.VoteDto
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.ConsumptionFrequency
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.ShopStatus
import com.vidrierapp.domain.model.TimeSlot
import com.vidrierapp.domain.model.Vote
import com.vidrierapp.domain.model.Weekday

fun ShopDto.toDomain(): Shop {
    return Shop(
        id = id,
        address = address,
        neighborhood = neighborhood,
        latitude = latitude,
        longitude = longitude,
        surfaceAreaM2 = surfaceAreaM2,
        rentalPrice = rentalPrice,
        topCategory = topCategoryName?.let { Category(id = it, name = it) },
        totalVotes = totalVotes,
        voteBreakdown = votes.map { it.toDomain() },
        contactAgency = contactAgency,
        contactPhone = contactPhone,
        contactEmail = contactEmail,
        firstVoteAt = firstVoteAt,
        lastVoteAt = lastVoteAt,
        frequencyCounts = frequencyCounts.toEnumMap<ConsumptionFrequency>(),
        votesByWeekday = votesByWeekday.toEnumMap<Weekday>(),
        votesByTimeSlot = votesByTimeSlot.toEnumMap<TimeSlot>(),
        status = status,
        listingStatus = ShopStatus.fromRaw(listingStatus)
    )
}

fun VoteDto.toDomain(): Vote {
    return Vote(
        category = Category(id = categoryId, name = categoryName),
        count = voteCount,
        percentage = percentage
    )
}

private inline fun <reified E : Enum<E>> Map<String, Int>.toEnumMap(): Map<E, Int> =
    entries.mapNotNull { (key, count) ->
        enumValues<E>().firstOrNull { it.name == key }?.let { it to count }
    }.toMap()
