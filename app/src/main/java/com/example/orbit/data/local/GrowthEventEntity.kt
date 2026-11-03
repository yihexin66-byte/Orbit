package com.example.orbit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.orbit.model.GrowthEvent
import com.example.orbit.model.GrowthEventType

@Entity(
    tableName = "growth_events",
    indices = [
        Index(value = ["sourceKey"], name = "index_growth_events_sourceKey", unique = true),
        Index(value = ["type"], name = "index_growth_events_type"),
        Index(value = ["epochDay"], name = "index_growth_events_epochDay"),
    ],
)
data class GrowthEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: GrowthEventType,
    val sourceKey: String,
    val xp: Int,
    val occurredAt: Long,
    val epochDay: Long,
)

internal fun GrowthEventEntity.toModel(): GrowthEvent = GrowthEvent(
    id = id,
    type = type,
    sourceKey = sourceKey,
    xp = xp,
    occurredAt = occurredAt,
    epochDay = epochDay,
)
