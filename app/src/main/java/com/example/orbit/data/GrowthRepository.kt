package com.example.orbit.data

import com.example.orbit.data.local.GrowthEventEntity
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.toModel
import com.example.orbit.model.GrowthEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read-only view of the append-only growth ledger. Rewards are written by the
 * repositories that own their source (tasks, habits, stages, goals).
 */
class GrowthRepository(private val database: OrbitDatabase) {

    fun observeEvents(): Flow<List<GrowthEvent>> =
        database.growthDao().observeAll().map { entities ->
            entities.map(GrowthEventEntity::toModel)
        }
}
