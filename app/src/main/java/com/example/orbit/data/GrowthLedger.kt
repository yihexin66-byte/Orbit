package com.example.orbit.data

import com.example.orbit.data.local.GrowthDao
import com.example.orbit.data.local.GrowthEventEntity
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.TaskDate

/**
 * Append-only XP bookkeeping shared by every repository. The unique source key
 * makes a reward idempotent: completing, reopening and completing again never
 * grants the same achievement twice.
 */
internal class GrowthLedger(private val growthDao: GrowthDao) {

    suspend fun record(
        type: GrowthEventType,
        sourceKey: String,
        xp: Int,
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        growthDao.insertIgnore(
            GrowthEventEntity(
                type = type,
                sourceKey = sourceKey,
                xp = xp,
                occurredAt = occurredAt,
                epochDay = TaskDate.todayEpochDay(occurredAt),
            ),
        )
    }
}
