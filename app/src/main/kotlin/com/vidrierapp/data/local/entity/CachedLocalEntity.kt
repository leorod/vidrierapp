package com.vidrierapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_locals")
data class CachedLocalEntity(
    @PrimaryKey
    val shopId: String,
    val payloadJson: String,
    val cachedAt: Long
)
