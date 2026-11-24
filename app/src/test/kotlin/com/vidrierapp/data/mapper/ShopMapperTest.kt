package com.vidrierapp.data.mapper

import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.data.remote.dto.VoteDto
import com.vidrierapp.domain.model.ConsumptionFrequency
import com.vidrierapp.domain.model.TimeSlot
import com.vidrierapp.domain.model.Weekday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopMapperTest {

    @Test
    fun `map ShopDto to Domain Shop correctly`() {
        val dto = ShopDto(
            id = "shop-1",
            address = "Av. Rivadavia 1000",
            neighborhood = "Haedo",
            latitude = -34.64,
            longitude = -58.60,
            surfaceAreaM2 = 50,
            rentalPrice = "$500,000",
            topCategoryName = "Cafetería",
            totalVotes = 20,
            votes = listOf(
                VoteDto(categoryId = "cat-1", categoryName = "Cafetería", voteCount = 20, percentage = 100f)
            ),
            contactAgency = "Inmobiliaria Test",
            status = "SUFFICIENT"
        )

        val domain = dto.toDomain()

        assertEquals("shop-1", domain.id)
        assertEquals("Av. Rivadavia 1000", domain.address)
        assertEquals("Haedo", domain.neighborhood)
        assertEquals("Cafetería", domain.topCategory?.name)
        assertEquals(20, domain.totalVotes)
        assertEquals("SUFFICIENT", domain.status)
        assertEquals(1, domain.voteBreakdown.size)
    }

    private fun detailDto(
        frequencyCounts: Map<String, Int> = emptyMap(),
        votesByWeekday: Map<String, Int> = emptyMap(),
        votesByTimeSlot: Map<String, Int> = emptyMap()
    ) = ShopDto(
        id = "shop-1",
        address = "Av. Rivadavia 1000",
        neighborhood = "Haedo",
        latitude = -34.64,
        longitude = -58.60,
        surfaceAreaM2 = 50,
        rentalPrice = "$500,000",
        totalVotes = 20,
        firstVoteAt = 1_000L,
        lastVoteAt = 2_000L,
        frequencyCounts = frequencyCounts,
        votesByWeekday = votesByWeekday,
        votesByTimeSlot = votesByTimeSlot
    )

    @Test
    fun `maps vote dates and distributions to domain enums`() {
        val domain = detailDto(
            frequencyCounts = mapOf("DIARIA" to 10, "SEMANAL" to 5, "OCASIONAL" to 2),
            votesByWeekday = mapOf("MONDAY" to 4, "SUNDAY" to 6),
            votesByTimeSlot = mapOf("MORNING" to 12, "NIGHT" to 8)
        ).toDomain()

        assertEquals(1_000L, domain.firstVoteAt)
        assertEquals(2_000L, domain.lastVoteAt)
        assertEquals(
            mapOf(
                ConsumptionFrequency.DIARIA to 10,
                ConsumptionFrequency.SEMANAL to 5,
                ConsumptionFrequency.OCASIONAL to 2
            ),
            domain.frequencyCounts
        )
        assertEquals(mapOf(Weekday.MONDAY to 4, Weekday.SUNDAY to 6), domain.votesByWeekday)
        assertEquals(mapOf(TimeSlot.MORNING to 12, TimeSlot.NIGHT to 8), domain.votesByTimeSlot)
    }

    @Test
    fun `ignores unknown distribution keys`() {
        val domain = detailDto(
            frequencyCounts = mapOf("DIARIA" to 1, "YEARLY" to 9),
            votesByWeekday = mapOf("FUNDAY" to 3, "FRIDAY" to 2),
            votesByTimeSlot = mapOf("DAWN" to 4)
        ).toDomain()

        assertEquals(mapOf(ConsumptionFrequency.DIARIA to 1), domain.frequencyCounts)
        assertEquals(mapOf(Weekday.FRIDAY to 2), domain.votesByWeekday)
        assertEquals(emptyMap<TimeSlot, Int>(), domain.votesByTimeSlot)
    }

    @Test
    fun `missing dates stay null`() {
        val domain = detailDto().copy(firstVoteAt = null, lastVoteAt = null).toDomain()

        assertNull(domain.firstVoteAt)
        assertNull(domain.lastVoteAt)
    }
}
