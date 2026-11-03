package com.example.orbit.ai

import com.example.orbit.model.TaskPriority

/**
 * Wire models for the Orbit AI gateway. Field names mirror the cloud function
 * contract; every field has a default so a partial response cannot crash Gson.
 */
data class AiStatusResponse(
    val configured: Boolean = false,
    val planPromptVersion: String? = null,
    val reviewPromptVersion: String? = null,
    val chatPromptVersion: String? = null,
    val timeoutSeconds: Int = 25,
    val limits: AiLimits = AiLimits(),
    val usage: AiUsage = AiUsage(),
    val dayKey: String? = null,
) {
    fun toStatus(): AiStatus = AiStatus(
        configured = configured,
        planPromptVersion = planPromptVersion,
        reviewPromptVersion = reviewPromptVersion,
        chatPromptVersion = chatPromptVersion,
        timeoutSeconds = timeoutSeconds,
        limits = limits,
        usage = usage,
        dayKey = dayKey,
    )
}

data class AiLimits(
    val plan: Int = 6,
    val review: Int = 4,
    val chat: Int = 20,
)

data class AiUsage(
    val planUsed: Int = 0,
    val reviewUsed: Int = 0,
    val chatUsed: Int = 0,
)

data class AiStatus(
    val configured: Boolean = false,
    val planPromptVersion: String? = null,
    val reviewPromptVersion: String? = null,
    val chatPromptVersion: String? = null,
    val timeoutSeconds: Int = 25,
    val limits: AiLimits = AiLimits(),
    val usage: AiUsage = AiUsage(),
    val dayKey: String? = null,
) {
    val planRemaining: Int get() = (limits.plan - usage.planUsed).coerceAtLeast(0)
    val reviewRemaining: Int get() = (limits.review - usage.reviewUsed).coerceAtLeast(0)
    val chatRemaining: Int get() = (limits.chat - usage.chatUsed).coerceAtLeast(0)
}

data class AiPlanRequest(
    val goal: String,
    val targetDateEpochDay: Long,
    val dailyMinutes: Int,
    val currentLevel: String = "",
    val weakPoints: String = "",
)

data class AiPlanResponse(
    val requestId: String = "",
    val generatedAt: Long = 0L,
    val promptVersion: String? = null,
    val plan: AiPlan? = null,
)

data class AiPlan(
    val goalTitle: String = "",
    val goalDescription: String = "",
    val targetDateEpochDay: Long = 0L,
    val stages: List<AiPlanStage> = emptyList(),
) {
    val taskCount: Int get() = stages.sumOf { it.tasks.size }
}

data class AiPlanStage(
    val title: String = "",
    val targetDateEpochDay: Long = 0L,
    val tasks: List<AiPlanTask> = emptyList(),
)

data class AiPlanTask(
    val title: String = "",
    val dueDateEpochDay: Long = 0L,
    val priority: String = "MEDIUM",
    val description: String = "",
) {
    fun priorityValue(): TaskPriority = when (priority.trim().uppercase()) {
        "HIGH" -> TaskPriority.HIGH
        "LOW" -> TaskPriority.LOW
        else -> TaskPriority.MEDIUM
    }
}

data class AiReviewGoal(
    val title: String,
    val progress: Float,
    val stageCount: Int,
    val completedStages: Int,
)

data class AiReviewRequest(
    val weekStartEpochDay: Long,
    val planned: Int,
    val completed: Int,
    val studyPlanned: Int,
    val studyCompleted: Int,
    val habitPossible: Int,
    val habitCheckIns: Int,
    val xp: Int,
    val goals: List<AiReviewGoal> = emptyList(),
) {
    /** Mirrors the server-side guard so the button is never a guaranteed 400. */
    val hasData: Boolean
        get() = planned > 0 || habitPossible > 0 || xp > 0 || goals.isNotEmpty()
}

data class AiReviewResponse(
    val requestId: String = "",
    val generatedAt: Long = 0L,
    val promptVersion: String? = null,
    val review: AiReview? = null,
)

data class AiReview(
    val headline: String = "",
    val summary: String = "",
    val wins: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val nextWeek: List<AiReviewAction> = emptyList(),
)

data class AiReviewAction(
    val title: String = "",
    val reason: String = "",
)

/** The review is per account and per week; nothing else is cached locally. */
data class CachedAiReview(
    val requestId: String = "",
    val generatedAt: Long = 0L,
    val promptVersion: String? = null,
    val review: AiReview = AiReview(),
)

data class AiChatTurn(
    val role: String = "user",
    val content: String = "",
)

enum class AiChatRole {
    USER,
    ASSISTANT,
}

data class AiChatMessage(
    val id: Long,
    val role: AiChatRole,
    val text: String,
)

data class AiChatGoal(
    val title: String,
    val progress: Float,
)

/**
 * Only aggregate numbers and goal titles leave the device. Task notes, habit
 * names and the account nickname never enter the chat request.
 */
data class AiChatContext(
    val todayPlanned: Int = 0,
    val todayCompleted: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val habitTodayChecked: Int = 0,
    val habitTodayTotal: Int = 0,
    val totalXp: Int = 0,
    val thisWeekXp: Int = 0,
    val goals: List<AiChatGoal> = emptyList(),
)

data class AiChatRequest(
    val message: String,
    val history: List<AiChatTurn> = emptyList(),
    val context: AiChatContext = AiChatContext(),
)

data class AiChatResponse(
    val requestId: String = "",
    val generatedAt: Long = 0L,
    val promptVersion: String? = null,
    val reply: String = "",
)
