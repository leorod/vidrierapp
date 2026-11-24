package com.vidrierapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey
    val id: String,
    val shopId: String?,
    val timestamp: Long,
    val notes: String,
    val photoUrisCsv: String = "",
    val pedestrianCount: Int = 0,
    val manualAddress: String? = null
)
