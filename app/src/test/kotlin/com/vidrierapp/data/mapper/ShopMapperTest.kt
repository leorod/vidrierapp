package com.vidrierapp.data.mapper

import com.vidrierapp.data.remote.dto.ShopDto
import com.vidrierapp.data.remote.dto.VoteDto
import org.junit.Assert.assertEquals
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
}
