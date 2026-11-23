package com.vidrierapp.data.mapper

import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.data.remote.dto.VoteDto
import com.vidrierapp.domain.model.Category
import com.vidrierapp.domain.model.Shop
import com.vidrierapp.domain.model.Vote

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
        lastVoteDate = lastVoteDate,
        dailyFrequencyCount = dailyFrequencyCount,
        morningTimeSlotPercentage = morningTimeSlotPercentage,
        status = status
    )
}

fun VoteDto.toDomain(): Vote {
    return Vote(
        category = Category(id = categoryId, name = categoryName),
        count = voteCount,
        percentage = percentage
    )
}
