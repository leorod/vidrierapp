package com.vidrierapp.data.mapper

import com.vidrierapp.data.local.entity.VisitEntity
import com.vidrierapp.domain.model.Visit
import org.junit.Assert.assertEquals
import org.junit.Test

class VisitMapperTest {

    private fun visit(photoUris: List<String>) = Visit(
        id = "v1",
        shopId = "s1",
        timestamp = 1234L,
        notes = "notes",
        photoUris = photoUris,
        pedestrianCount = 12
    )

    @Test
    fun `round trip with no photos`() {
        val visit = visit(emptyList())
        val entity = visit.toEntity()

        assertEquals("[]", entity.photoPaths)
        assertEquals(visit, entity.toDomain())
    }

    @Test
    fun `round trip with one photo`() {
        val visit = visit(listOf("content://a"))
        val entity = visit.toEntity()

        assertEquals("[\"content://a\"]", entity.photoPaths)
        assertEquals(visit, entity.toDomain())
    }

    @Test
    fun `round trip with two photos`() {
        val visit = visit(listOf("content://a", "content://b"))
        val entity = visit.toEntity()

        assertEquals("[\"content://a\",\"content://b\"]", entity.photoPaths)
        assertEquals(visit, entity.toDomain())
    }

    @Test
    fun `paths containing commas survive the round trip`() {
        val visit = visit(listOf("/data/a,b.jpg", "/data/c.jpg"))

        assertEquals(visit, visit.toEntity().toDomain())
    }

    @Test
    fun `blank stored paths decode to an empty list`() {
        val entity = visit(emptyList()).toEntity().copy(photoPaths = "")

        assertEquals(emptyList<String>(), entity.toDomain().photoUris)
    }

    @Test
    fun `entity to domain maps all fields`() {
        val entity = VisitEntity(
            id = "v9",
            shopId = "s9",
            timestamp = 99L,
            notes = "n",
            photoPaths = "[\"x\",\"y\"]",
            pedestrianCount = 3
        )

        assertEquals(
            Visit("v9", "s9", 99L, "n", listOf("x", "y"), 3),
            entity.toDomain()
        )
    }
}
