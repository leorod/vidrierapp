package com.vidrierapp.data.mapper

import com.vidrierapp.data.local.entity.VisitEntity
import com.vidrierapp.domain.model.Visit
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val photoPathsSerializer = ListSerializer(String.serializer())

fun VisitEntity.toDomain(): Visit {
    return Visit(
        id = id,
        shopId = shopId,
        timestamp = timestamp,
        notes = notes,
        photoUris = decodePhotoPaths(photoPaths),
        pedestrianCount = pedestrianCount,
        manualAddress = manualAddress
    )
}

fun Visit.toEntity(): VisitEntity {
    return VisitEntity(
        id = id,
        shopId = shopId,
        timestamp = timestamp,
        notes = notes,
        photoPaths = Json.encodeToString(photoPathsSerializer, photoUris),
        pedestrianCount = pedestrianCount,
        manualAddress = manualAddress
    )
}

private fun decodePhotoPaths(raw: String): List<String> =
    if (raw.isBlank()) emptyList() else Json.decodeFromString(photoPathsSerializer, raw)
