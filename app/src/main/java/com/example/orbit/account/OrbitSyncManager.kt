package com.example.orbit.account

import android.os.SystemClock
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import com.example.orbit.data.local.GrowthEventEntity
import com.example.orbit.data.local.GoalEntity
import com.example.orbit.data.local.GoalStageEntity
import com.example.orbit.data.local.HabitEntity
import com.example.orbit.data.local.HabitRecordEntity
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.data.local.TaskEntity
import com.example.orbit.model.AccentTone
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.GrowthEventType
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskPriority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps the single local Room database in step with the MySQL copy owned by the
 * signed-in account.
 *
 * MySQL is the authority: the UI cannot exist without a session, the cache is
 * replaced by the account snapshot right after login, and logout clears it.
 * While signed in, every committed write marks the cache dirty and schedules a
 * debounced push. A pull happens on sign-in, on manual refresh and on app start.
 * The server is authoritative on conflicts: a rejected push receives the newer
 * snapshot and that snapshot replaces the local cache.
 *
 * Local data is tagged with [AccountStore.localOwnerUserId]. Pre-account rows
 * are discarded by [prepareForAccount] because they belong to no cloud account;
 * signing into a different account therefore never leaks the previous user's
 * rows.
 */
class OrbitSyncManager(
    private val database: OrbitDatabase,
    private val store: AccountStore,
    private val repository: AccountRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val pushSignals = Channel<Unit>(Channel.CONFLATED)

    private val _status = MutableStateFlow(
        SyncStatus(lastSyncedAt = store.lastSyncedAt.takeIf { it > 0L }),
    )
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    @Volatile
    private var applyingRemote = false

    @Volatile
    private var suppressInvalidationUntil = 0L

    private var started = false

    private val observer = object : InvalidationTracker.Observer(
        "goals",
        "goal_stages",
        "tasks",
        "habits",
        "habit_records",
        "growth_events",
    ) {
        override fun onInvalidated(tables: Set<String>) {
            onLocalTablesChanged()
        }
    }

    fun start() {
        if (started) return
        started = true
        // Registering the observer initializes Room's invalidation tracker, which
        // opens the database; keep that off the main thread.
        scope.launch {
            database.invalidationTracker.addObserver(observer)
            for (signal in pushSignals) {
                signal
                kotlinx.coroutines.delay(PUSH_DEBOUNCE_MS)
                performSync()
            }
        }
        if (!store.syncBlocked && store.session.value != null) {
            requestSync()
        }
    }

    /** Called after the account store saved a new session. */
    fun onSignedIn() {
        if (store.syncBlocked) return
        _status.value = SyncStatus(phase = SyncPhase.SYNCING)
        requestSync()
    }

    /** Called after the account store cleared the session. */
    fun onSignedOut() {
        _status.value = SyncStatus()
    }

    fun requestSync() {
        if (store.syncBlocked) return
        scope.launch { performSync() }
    }

    /**
     * Suspending variant used by the login gate: the caller waits until the
     * account snapshot has replaced the cache (or the pull failed) before the
     * editable UI appears.
     */
    suspend fun syncNow() {
        if (store.syncBlocked) return
        withContext(Dispatchers.IO) { performSync() }
    }

    /**
     * Drops rows owned by a different (or previous) account before a new
     * session is saved. A fresh install has no owner, and its pre-account rows
     * are intentionally discarded: MySQL is the authority now.
     */
    suspend fun prepareForAccount(userId: Long) {
        // Called from the login coroutine on the main dispatcher; Room's
        // clearAllTables is blocking, so the cache clear must leave the UI thread.
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (store.localOwnerUserId != userId) {
                    clearLocalCacheLocked()
                }
                _status.value = SyncStatus(phase = SyncPhase.SYNCING)
            }
        }
    }

    /** Deletes the local cache and ownership after logout or an expired token. */
    suspend fun clearLocalCache() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                clearLocalCacheLocked()
            }
        }
        _status.value = SyncStatus()
    }

    private suspend fun clearLocalCacheLocked() {
        applyingRemote = true
        suppressInvalidationUntil = SystemClock.elapsedRealtime() + APPLY_SUPPRESSION_MS
        try {
            database.clearAllTables()
            store.resetLocalOwnership()
        } finally {
            applyingRemote = false
            suppressInvalidationUntil = SystemClock.elapsedRealtime() + APPLY_SUPPRESSION_MS
        }
    }

    private fun onLocalTablesChanged() {
        if (applyingRemote) return
        if (store.syncBlocked) return
        if (SystemClock.elapsedRealtime() < suppressInvalidationUntil) return
        val owner = store.localOwnerUserId
        if (owner == AccountStore.NO_OWNER) return
        store.localDirty = true
        if (store.userId == owner) {
            pushSignals.trySend(Unit)
        }
    }

    private suspend fun performSync() {
        if (store.syncBlocked) return
        val session = store.session.value ?: return
        if (applyingRemote) return
        mutex.withLock {
            if (!isCurrent(session)) return
            _status.value = SyncStatus(
                phase = SyncPhase.SYNCING,
                lastSyncedAt = store.lastSyncedAt.takeIf { it > 0L },
            )
            try {
                val remote = repository.pull(session.token)
                if (!isCurrent(session)) return
                val owner = store.localOwnerUserId
                val localRevision = store.syncRevision
                when {
                    // Only this account's own local edits are ever pushed.
                    owner == session.user.id && store.localDirty ->
                        push(session, localRevision)

                    owner == session.user.id -> {
                        if (remote.revision > localRevision) {
                            applyRemote(session.user.id, remote)
                            markSynced()
                        } else {
                            markSynced()
                        }
                    }

                    // No owner, or another account's rows: the MySQL snapshot
                    // always wins and replaces the cache, even when it is empty.
                    else -> {
                        applyRemote(session.user.id, remote)
                        markSynced()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: OrbitApiException) {
                handleFailure(failure)
            } catch (failure: Exception) {
                handleFailure(
                    OrbitApiException(
                        statusCode = 0,
                        code = "SYNC_FAILED",
                        message = failure.message ?: "同步失败",
                    ),
                )
            }
        }
    }

    private suspend fun push(session: StoredAccount, baseRevision: Long) {
        val request = readLocalSnapshot().toPushRequest(baseRevision)
        val result = repository.push(session.token, request)
        if (!isCurrent(session)) return
        if (result.accepted) {
            store.syncRevision = result.snapshot.revision
            store.localOwnerUserId = session.user.id
            store.localDirty = false
            markSynced()
        } else {
            applyRemote(session.user.id, result.snapshot)
            val now = System.currentTimeMillis()
            store.lastSyncedAt = now
            _status.value = SyncStatus(
                phase = SyncPhase.CONFLICT,
                lastSyncedAt = now,
                message = "云端已有更新，已采用云端版本",
            )
        }
    }

    private suspend fun applyRemote(userId: Long, snapshot: SyncSnapshot) {
        applyingRemote = true
        suppressInvalidationUntil = SystemClock.elapsedRealtime() + APPLY_SUPPRESSION_MS
        try {
            database.withTransaction {
                // Delete children before parents and insert parents first so the
                // foreign keys stay satisfied at every point of the transaction.
                database.taskDao().deleteAll()
                database.habitRecordDao().deleteAll()
                database.growthDao().deleteAll()
                database.goalStageDao().deleteAll()
                database.habitDao().deleteAll()
                database.goalDao().deleteAll()

                database.goalDao().insertAll(snapshot.goals.map { it.toEntity() })
                database.goalStageDao().insertAll(snapshot.stages.map { it.toEntity() })
                database.habitDao().insertAll(snapshot.habits.map { it.toEntity() })
                database.taskDao().insertAll(snapshot.tasks.map { it.toEntity() })
                database.habitRecordDao().insertAll(snapshot.habitRecords.map { it.toEntity() })
                database.growthDao().insertAll(snapshot.growthEvents.map { it.toEntity() })
            }
            store.syncRevision = snapshot.revision
            store.localOwnerUserId = userId
            store.localDirty = false
        } finally {
            applyingRemote = false
            suppressInvalidationUntil = SystemClock.elapsedRealtime() + APPLY_SUPPRESSION_MS
        }
    }

    private fun markSynced() {
        val now = System.currentTimeMillis()
        store.lastSyncedAt = now
        _status.value = SyncStatus(phase = SyncPhase.SYNCED, lastSyncedAt = now)
    }

    private suspend fun handleFailure(failure: OrbitApiException) {
        if (failure.statusCode == 401) {
            store.clearSession()
            clearLocalCacheLocked()
            _status.value = SyncStatus(
                phase = SyncPhase.OFFLINE,
                message = "登录已过期，请重新登录",
            )
            return
        }
        _status.value = SyncStatus(
            phase = SyncPhase.OFFLINE,
            lastSyncedAt = store.lastSyncedAt.takeIf { it > 0L },
            message = failure.message,
        )
    }

    private fun isCurrent(session: StoredAccount): Boolean = store.token == session.token

    private suspend fun readLocalSnapshot(): SyncSnapshot = SyncSnapshot(
        revision = store.syncRevision,
        goals = database.goalDao().getAll().map { it.toSync() },
        stages = database.goalStageDao().getAll().map { it.toSync() },
        tasks = database.taskDao().getAll().map { it.toSync() },
        habits = database.habitDao().getAll().map { it.toSync() },
        habitRecords = database.habitRecordDao().getAll().map { it.toSync() },
        growthEvents = database.growthDao().getAll().map { it.toSync() },
    )

    private companion object {
        const val PUSH_DEBOUNCE_MS = 1_200L

        /** Room delivers invalidations after commit; ignore both phases. */
        const val APPLY_SUPPRESSION_MS = 1_500L
    }
}

private fun SyncSnapshot.toPushRequest(baseRevision: Long): SyncPushRequest = SyncPushRequest(
    baseRevision = baseRevision,
    goals = goals,
    stages = stages,
    tasks = tasks,
    habits = habits,
    habitRecords = habitRecords,
    growthEvents = growthEvents,
)

private fun GoalEntity.toSync(): SyncGoal = SyncGoal(
    id = id,
    title = title,
    description = description,
    tone = tone.name,
    status = status.name,
    targetDate = targetDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    sortOrder = sortOrder,
)

private fun GoalStageEntity.toSync(): SyncStage = SyncStage(
    id = id,
    goalId = goalId,
    title = title,
    position = position,
    targetDate = targetDate,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun TaskEntity.toSync(): SyncTask = SyncTask(
    id = id,
    title = title,
    description = description,
    priority = priority.name,
    category = category.name,
    dueDate = dueDate,
    isCompleted = isCompleted,
    createdAt = createdAt,
    completedAt = completedAt,
    goalId = goalId,
    stageId = stageId,
)

private fun HabitEntity.toSync(): SyncHabit = SyncHabit(
    id = id,
    title = title,
    note = note,
    tone = tone.name,
    createdAt = createdAt,
    archivedAt = archivedAt,
    sortOrder = sortOrder,
)

private fun HabitRecordEntity.toSync(): SyncHabitRecord = SyncHabitRecord(
    id = id,
    habitId = habitId,
    epochDay = epochDay,
    checkedAt = checkedAt,
)

private fun GrowthEventEntity.toSync(): SyncGrowthEvent = SyncGrowthEvent(
    id = id,
    type = type.name,
    sourceKey = sourceKey,
    xp = xp,
    occurredAt = occurredAt,
    epochDay = epochDay,
)

private fun SyncGoal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    description = description,
    tone = tone.toAccentTone(),
    status = status.toGoalStatus(),
    targetDate = targetDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    sortOrder = sortOrder,
)

private fun SyncStage.toEntity(): GoalStageEntity = GoalStageEntity(
    id = id,
    goalId = goalId,
    title = title,
    position = position,
    targetDate = targetDate,
    isCompleted = isCompleted,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun SyncTask.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    priority = priority.toTaskPriority(),
    category = category.toTaskCategory(),
    dueDate = dueDate,
    isCompleted = isCompleted,
    createdAt = createdAt,
    completedAt = completedAt,
    goalId = goalId,
    stageId = stageId,
)

private fun SyncHabit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title,
    note = note,
    tone = tone.toAccentTone(),
    createdAt = createdAt,
    archivedAt = archivedAt,
    sortOrder = sortOrder,
)

private fun SyncHabitRecord.toEntity(): HabitRecordEntity = HabitRecordEntity(
    id = id,
    habitId = habitId,
    epochDay = epochDay,
    checkedAt = checkedAt,
)

private fun SyncGrowthEvent.toEntity(): GrowthEventEntity = GrowthEventEntity(
    id = id,
    type = type.toGrowthEventType(),
    sourceKey = sourceKey,
    xp = xp,
    occurredAt = occurredAt,
    epochDay = epochDay,
)

private fun String?.toAccentTone(): AccentTone =
    AccentTone.entries.firstOrNull { it.name == this } ?: AccentTone.CYAN

private fun String?.toGoalStatus(): GoalStatus =
    GoalStatus.entries.firstOrNull { it.name == this } ?: GoalStatus.ACTIVE

private fun String?.toTaskPriority(): TaskPriority =
    TaskPriority.entries.firstOrNull { it.name == this } ?: TaskPriority.MEDIUM

private fun String?.toTaskCategory(): TaskCategory =
    TaskCategory.entries.firstOrNull { it.name == this } ?: TaskCategory.OTHER

private fun String?.toGrowthEventType(): GrowthEventType =
    GrowthEventType.entries.firstOrNull { it.name == this }
        ?: GrowthEventType.TASK_COMPLETED
