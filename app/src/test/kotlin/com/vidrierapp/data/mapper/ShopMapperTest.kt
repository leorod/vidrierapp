package com.vidrierapp.data.mapper

import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.data.remote.dto.VoteDto
import com.vidrierapp.domain.model.ConsumptionFrequency
import com.vidrierapp.domain.model.TimeSlot
import com.vidrierapp.domain.model.Weekday
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopMapperTest {

    @Test
    fun `decodes a shop listed without neighborhood, surface or rent`() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val dto = json.decodeFromString<ShopDto>(
            """{"id":"s9","address":"Calle Falsa 123","latitude":-34.66,"longitude":-58.51,"status":"AVAILABLE"}"""
        )

        assertEquals("", dto.neighborhood)
        assertEquals(0, dto.surfaceAreaM2)
        assertEquals("", dto.rentalPrice)
    }

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
            contactPhone = "+54 11 4000-0000",
            contactEmail = "a@b.example",
            status = "SUFFICIENT"
        )

        val domain = dto.toDomain()

        assertEquals("shop-1", domain.id)
        assertEquals("Av. Rivadavia 1000", domain.address)
        assertEquals("Haedo", domain.neighborhood)
        assertEquals("Cafetería", domain.topCategory?.name)
        assertEquals(20, domain.totalVotes)
        assertEquals("SUFFICIENT", domain.status)
        assertEquals("+54 11 4000-0000", domain.contactPhone)
        assertEquals("a@b.example", domain.contactEmail)
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

    @Test
    fun `missing listing status in stale cached json defaults to ACTIVE`() {
        val stale = """{"id":"s","address":"a","neighborhood":"n","latitude":0.0,"longitude":0.0,"surfaceAreaM2":1,"rentalPrice":"1"}"""
        val dto = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(ShopDto.serializer(), stale)

        assertEquals(com.vidrierapp.domain.model.ShopStatus.ACTIVE, dto.toDomain().listingStatus)
    }

    @Test
    fun `listing status maps known values and falls back to ACTIVE for unknown`() {
        val base = detailDto()

        assertEquals(
            com.vidrierapp.domain.model.ShopStatus.UNDER_REVIEW,
            base.copy(listingStatus = "UNDER_REVIEW").toDomain().listingStatus
        )
        assertEquals(
            com.vidrierapp.domain.model.ShopStatus.RENTED,
            base.copy(listingStatus = "RENTED").toDomain().listingStatus
        )
        assertEquals(
            com.vidrierapp.domain.model.ShopStatus.ACTIVE,
            base.copy(listingStatus = "SOMETHING_NEW").toDomain().listingStatus
        )
    }
}
