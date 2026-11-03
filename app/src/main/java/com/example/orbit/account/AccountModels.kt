package com.example.orbit.account

/** Account-facing DTOs. Field names mirror the Spring Boot JSON contract. */
data class AccountUser(
    val id: Long,
    val username: String,
    val nickname: String,
    val avatarPath: String? = null,
    val createdAt: Long = 0L,
)

data class StoredAccount(
    val token: String,
    val user: AccountUser,
)

data class AuthRequest(
    val username: String,
    val password: String,
)

data class AuthResponse(
    val token: String,
    val user: AccountUser,
)

data class NicknameRequest(
    val nickname: String,
)

/**
 * JSON avatar payload. uniCloud URL化 does not forward multipart bodies to the
 * cloud function, so the JPEG is base64 encoded and uploaded as JSON.
 */
data class AvatarUploadRequest(
    val contentBase64: String,
    val contentType: String = "image/jpeg",
)

data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
)

data class SyncGoal(
    val id: Long,
    val title: String,
    val description: String? = null,
    val tone: String,
    val status: String,
    val targetDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
)

data class SyncStage(
    val id: Long,
    val goalId: Long,
    val title: String,
    val position: Int,
    val targetDate: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class SyncTask(
    val id: Long,
    val title: String,
    val description: String? = null,
    val priority: String,
    val category: String,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val completedAt: Long? = null,
    val goalId: Long? = null,
    val stageId: Long? = null,
)

data class SyncHabit(
    val id: Long,
    val title: String,
    val note: String? = null,
    val tone: String,
    val createdAt: Long,
    val archivedAt: Long? = null,
    val sortOrder: Int = 0,
)

data class SyncHabitRecord(
    val id: Long,
    val habitId: Long,
    val epochDay: Long,
    val checkedAt: Long,
)

data class SyncGrowthEvent(
    val id: Long,
    val type: String,
    val sourceKey: String,
    val xp: Int,
    val occurredAt: Long,
    val epochDay: Long,
)

data class SyncSnapshot(
    val revision: Long = 0L,
    val goals: List<SyncGoal> = emptyList(),
    val stages: List<SyncStage> = emptyList(),
    val tasks: List<SyncTask> = emptyList(),
    val habits: List<SyncHabit> = emptyList(),
    val habitRecords: List<SyncHabitRecord> = emptyList(),
    val growthEvents: List<SyncGrowthEvent> = emptyList(),
)

data class SyncPushRequest(
    val baseRevision: Long,
    val goals: List<SyncGoal>,
    val stages: List<SyncStage>,
    val tasks: List<SyncTask>,
    val habits: List<SyncHabit>,
    val habitRecords: List<SyncHabitRecord>,
    val growthEvents: List<SyncGrowthEvent>,
)

data class SyncPushResult(
    val accepted: Boolean,
    val snapshot: SyncSnapshot,
)

enum class SyncPhase {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    CONFLICT,
}

data class SyncStatus(
    val phase: SyncPhase = SyncPhase.IDLE,
    val lastSyncedAt: Long? = null,
    val message: String? = null,
)
