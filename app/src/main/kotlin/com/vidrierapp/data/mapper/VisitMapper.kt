package com.vidrierapp.data.mapper

import com.vidrierapp.data.local.entity.VisitEntity
import com.vidrierapp.domain.model.Visit

fun VisitEntity.toDomain(): Visit {
    return Visit(
        id = id,
        shopId = shopId,
        timestamp = timestamp,
        notes = notes,
        photoUris = if (photoUrisCsv.isBlank()) emptyList() else photoUrisCsv.split(","),
        pedestrianCount = pedestrianCount
    )
}

fun Visit.toEntity(): VisitEntity {
    return VisitEntity(
        id = id,
        shopId = shopId,
        timestamp = timestamp,
        notes = notes,
        photoUrisCsv = photoUris.joinToString(","),
        pedestrianCount = pedestrianCount
    )
}
