package com.example.orbit.account

import android.content.Context
import com.example.orbit.data.local.OrbitDatabase

/**
 * Application-scoped wiring for accounts and sync. The Compose layer creates
 * ViewModels per activity, but the database, session store and sync loop must
 * outlive configuration changes, so they are built once here.
 */
object OrbitAccountGraph {

    @Volatile
    private var initialized = false

    lateinit var store: AccountStore
        private set

    lateinit var repository: AccountRepository
        private set

    lateinit var syncManager: OrbitSyncManager
        private set

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val store = AccountStore(context)
            val database = OrbitDatabase.getInstance(context)
            val repository = AccountRepository()
            val syncManager = OrbitSyncManager(database, store, repository)
            this.store = store
            this.repository = repository
            this.syncManager = syncManager
            initialized = true
            syncManager.start()
        }
    }
}
