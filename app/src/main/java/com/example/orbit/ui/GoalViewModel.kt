package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.data.GoalRepository
import com.example.orbit.data.TaskRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.AccentTone
import com.example.orbit.model.Goal
import com.example.orbit.model.GoalDeletionInfo
import com.example.orbit.model.GoalMetrics
import com.example.orbit.model.GoalProgress
import com.example.orbit.model.GoalStage
import com.example.orbit.model.GoalStatus
import com.example.orbit.model.GoalTitleRules
import com.example.orbit.model.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GoalLoadStatus {
    LOADING,
    READY,
    FAILED,
}

data class GoalUiState(
    val goals: List<Goal> = emptyList(),
    val stages: List<GoalStage> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val loadStatus: GoalLoadStatus = GoalLoadStatus.LOADING,
    val feedback: GoalFeedback? = null,
) {
    val isLoading: Boolean get() = loadStatus == GoalLoadStatus.LOADING
    val loadFailed: Boolean get() = loadStatus == GoalLoadStatus.FAILED
    val hasGoals: Boolean get() = goals.isNotEmpty()

    val orderedGoals: List<Goal>
        get() = GoalMetrics.orderedGoals(goals)

    val activeGoals: List<Goal>
        get() = GoalMetrics.activeGoals(goals)

    val stageCount: Int get() = stages.size

    fun progressFor(goal: Goal): GoalProgress = GoalMetrics.progress(goal, stages, tasks)

    fun stagesFor(goalId: Long): List<GoalStage> =
        stages.filter { it.goalId == goalId }.sortedBy { it.position }

    fun goalById(goalId: Long): Goal? = goals.firstOrNull { it.id == goalId }

    fun tasksForGoal(goalId: Long): List<Task> =
        tasks.filter { it.goalId == goalId }.sortedWith(TaskOrder)

    /** Goal-level tasks are shown apart; they do not lift a stage average. */
    fun goalLevelTasks(goalId: Long): List<Task> =
        tasksForGoal(goalId).filter { it.stageId == null }

    fun tasksForStage(stageId: Long): List<Task> =
        tasks.filter { it.stageId == stageId }.sortedWith(TaskOrder)

    fun stageProgress(stage: GoalStage): Float = GoalMetrics.stageProgress(stage, tasks)
}

private val TaskOrder = compareBy<Task>({ it.isCompleted }, { it.createdAt }, { it.id })

data class GoalFeedback(
    val id: Long,
    val type: GoalFeedbackType,
)

enum class GoalFeedbackType {
    CREATED,
    UPDATED,
    DELETED,
    STATUS_UPDATED,
    STAGE_CREATED,
    STAGE_UPDATED,
    STAGE_DELETED,
    STAGE_COMPLETED,
    STAGE_REOPENED,
    SAVE_FAILED,
    DELETE_FAILED,
    STAGE_SAVE_FAILED,
    STAGE_DELETE_FAILED,
    STATUS_FAILED,
}

enum class GoalEditorActionError {
    SAVE_FAILED,
    DELETE_FAILED,
}

data class GoalDeletionState(
    val info: GoalDeletionInfo,
    val isDeleting: Boolean = false,
)

enum class StageEditorActionError {
    SAVE_FAILED,
    DELETE_FAILED,
}

data class StageEditorState(
    val goalId: Long,
    val stageId: Long? = null,
    val title: String = "",
    val targetDate: Long? = null,
    val linkedTaskCount: Int = 0,
    val titleError: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val actionError: StageEditorActionError? = null,
    val showDeleteConfirm: Boolean = false,
) {
    val isEditing: Boolean get() = stageId != null

    companion object {
        const val MAX_TITLE_LENGTH = GoalTitleRules.MAX_LENGTH
    }
}

data class GoalEditorState(
    val goalId: Long? = null,
    val title: String = "",
    val description: String = "",
    val tone: AccentTone = AccentTone.CYAN,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val targetDate: Long? = null,
    val titleError: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val actionError: GoalEditorActionError? = null,
    val showDeleteConfirm: Boolean = false,
) {
    val isEditing: Boolean get() = goalId != null

    companion object {
        const val MAX_TITLE_LENGTH = GoalTitleRules.MAX_LENGTH
        const val MAX_DESCRIPTION_LENGTH = 320
    }
}

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = OrbitDatabase.getInstance(application)
    private val goalRepository = GoalRepository(database)
    private val taskRepository = TaskRepository(database)

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    private val _editorState = MutableStateFlow<GoalEditorState?>(null)
    val editorState: StateFlow<GoalEditorState?> = _editorState.asStateFlow()

    private val _stageEditorState = MutableStateFlow<StageEditorState?>(null)
    val stageEditorState: StateFlow<StageEditorState?> = _stageEditorState.asStateFlow()

    private val _goalDeletion = MutableStateFlow<GoalDeletionState?>(null)
    val goalDeletion: StateFlow<GoalDeletionState?> = _goalDeletion.asStateFlow()

    private var observeJob: Job? = null
    private var feedbackCounter = 0L
    private val stageUpdatesInFlight = mutableSetOf<Long>()

    init {
        observeGoals()
    }

    fun retryLoad() {
        observeGoals()
    }

    private fun observeGoals() {
        observeJob?.cancel()
        _uiState.update { it.copy(loadStatus = GoalLoadStatus.LOADING) }

        observeJob = viewModelScope.launch {
            combine(
                goalRepository.observeGoals(),
                goalRepository.observeStages(),
                taskRepository.observeTasks(),
            ) { goals, stages, tasks -> Triple(goals, stages, tasks) }
                .catch {
                    _uiState.update { state -> state.copy(loadStatus = GoalLoadStatus.FAILED) }
                }
                .collect { (goals, stages, tasks) ->
                    _uiState.update { state ->
                        state.copy(
                            goals = goals,
                            stages = stages,
                            tasks = tasks,
                            loadStatus = GoalLoadStatus.READY,
                        )
                    }
                }
        }
    }

    fun openCreateGoal() {
        _editorState.value = GoalEditorState()
    }

    fun openEditGoal(goalId: Long) {
        val goal = _uiState.value.goals.firstOrNull { it.id == goalId } ?: return
        _editorState.value = GoalEditorState(
            goalId = goal.id,
            title = goal.title,
            description = goal.description.orEmpty(),
            tone = goal.tone,
            status = goal.status,
            targetDate = goal.targetDate,
        )
    }

    fun dismissEditor() {
        _editorState.value = null
    }

    fun updateEditorTitle(title: String) {
        _editorState.update { editor ->
            editor?.copy(
                title = title.take(GoalEditorState.MAX_TITLE_LENGTH),
                titleError = false,
                actionError = null,
            )
        }
    }

    fun updateEditorDescription(description: String) {
        _editorState.update { editor ->
            editor?.copy(
                description = description.take(GoalEditorState.MAX_DESCRIPTION_LENGTH),
                actionError = null,
            )
        }
    }

    fun updateEditorTone(tone: AccentTone) {
        _editorState.update { it?.copy(tone = tone) }
    }

    fun updateEditorStatus(status: GoalStatus) {
        _editorState.update { it?.copy(status = status) }
    }

    fun updateEditorTargetDate(targetDate: Long?) {
        _editorState.update { it?.copy(targetDate = targetDate) }
    }

    fun saveEditor() {
        val editor = _editorState.value ?: return
        if (editor.isSaving || editor.isDeleting) return
        if (!GoalTitleRules.isValid(editor.title)) {
            _editorState.update { it?.copy(titleError = true) }
            return
        }

        _editorState.update {
            it?.copy(isSaving = true, titleError = false, actionError = null)
        }

        viewModelScope.launch {
            try {
                if (editor.isEditing) {
                    val goalId = requireNotNull(editor.goalId)
                    val originalStatus = _uiState.value.goals
                        .firstOrNull { it.id == goalId }
                        ?.status
                    goalRepository.updateGoal(
                        id = goalId,
                        title = editor.title,
                        description = editor.description,
                        tone = editor.tone,
                        targetDate = editor.targetDate,
                    )
                    if (originalStatus != null && originalStatus != editor.status) {
                        goalRepository.setStatus(goalId, editor.status)
                    }
                } else {
                    goalRepository.createGoal(
                        title = editor.title,
                        description = editor.description,
                        tone = editor.tone,
                        targetDate = editor.targetDate,
                    )
                }
                _editorState.value = null
                emitFeedback(
                    if (editor.isEditing) GoalFeedbackType.UPDATED else GoalFeedbackType.CREATED,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _editorState.update {
                    it?.copy(isSaving = false, actionError = GoalEditorActionError.SAVE_FAILED)
                }
                emitFeedback(GoalFeedbackType.SAVE_FAILED)
            }
        }
    }

    fun requestDelete() {
        _editorState.update { it?.copy(showDeleteConfirm = true) }
    }

    fun dismissDeleteRequest() {
        _editorState.update { it?.copy(showDeleteConfirm = false) }
    }

    fun confirmDelete() {
        val editor = _editorState.value ?: return
        val goalId = editor.goalId ?: return
        if (editor.isSaving || editor.isDeleting) return

        _editorState.update {
            it?.copy(showDeleteConfirm = false, isDeleting = true, actionError = null)
        }

        viewModelScope.launch {
            try {
                goalRepository.deleteGoal(goalId)
                _editorState.value = null
                emitFeedback(GoalFeedbackType.DELETED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _editorState.update {
                    it?.copy(isDeleting = false, actionError = GoalEditorActionError.DELETE_FAILED)
                }
                emitFeedback(GoalFeedbackType.DELETE_FAILED)
            }
        }
    }

    // ---- Stages -----------------------------------------------------------

    fun openCreateStage(goalId: Long) {
        if (_uiState.value.goalById(goalId) == null) return
        _stageEditorState.value = StageEditorState(goalId = goalId)
    }

    fun openEditStage(stageId: Long) {
        val stage = _uiState.value.stages.firstOrNull { it.id == stageId } ?: return
        _stageEditorState.value = StageEditorState(
            goalId = stage.goalId,
            stageId = stage.id,
            title = stage.title,
            targetDate = stage.targetDate,
            linkedTaskCount = _uiState.value.tasksForStage(stage.id).size,
        )
    }

    fun dismissStageEditor() {
        _stageEditorState.value = null
    }

    fun updateStageTitle(title: String) {
        _stageEditorState.update { editor ->
            editor?.copy(
                title = title.take(StageEditorState.MAX_TITLE_LENGTH),
                titleError = false,
                actionError = null,
            )
        }
    }

    fun updateStageTargetDate(targetDate: Long?) {
        _stageEditorState.update { it?.copy(targetDate = targetDate) }
    }

    fun saveStageEditor() {
        val editor = _stageEditorState.value ?: return
        if (editor.isSaving || editor.isDeleting) return
        if (!GoalTitleRules.isValid(editor.title)) {
            _stageEditorState.update { it?.copy(titleError = true) }
            return
        }

        _stageEditorState.update {
            it?.copy(isSaving = true, titleError = false, actionError = null)
        }

        viewModelScope.launch {
            try {
                if (editor.isEditing) {
                    goalRepository.updateStage(
                        stageId = requireNotNull(editor.stageId),
                        title = editor.title,
                        targetDate = editor.targetDate,
                    )
                } else {
                    goalRepository.addStage(
                        goalId = editor.goalId,
                        title = editor.title,
                        targetDate = editor.targetDate,
                    )
                }
                _stageEditorState.value = null
                emitFeedback(
                    if (editor.isEditing) {
                        GoalFeedbackType.STAGE_UPDATED
                    } else {
                        GoalFeedbackType.STAGE_CREATED
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _stageEditorState.update {
                    it?.copy(
                        isSaving = false,
                        actionError = StageEditorActionError.SAVE_FAILED,
                    )
                }
                emitFeedback(GoalFeedbackType.STAGE_SAVE_FAILED)
            }
        }
    }

    fun requestDeleteStage() {
        _stageEditorState.update { it?.copy(showDeleteConfirm = true) }
    }

    fun dismissDeleteStage() {
        _stageEditorState.update { it?.copy(showDeleteConfirm = false) }
    }

    fun confirmDeleteStage() {
        val editor = _stageEditorState.value ?: return
        val stageId = editor.stageId ?: return
        if (editor.isSaving || editor.isDeleting) return

        _stageEditorState.update {
            it?.copy(showDeleteConfirm = false, isDeleting = true, actionError = null)
        }

        viewModelScope.launch {
            try {
                goalRepository.deleteStage(stageId)
                _stageEditorState.value = null
                emitFeedback(GoalFeedbackType.STAGE_DELETED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _stageEditorState.update {
                    it?.copy(
                        isDeleting = false,
                        actionError = StageEditorActionError.DELETE_FAILED,
                    )
                }
                emitFeedback(GoalFeedbackType.STAGE_DELETE_FAILED)
            }
        }
    }

    fun toggleStage(stageId: Long, completed: Boolean) {
        if (!stageUpdatesInFlight.add(stageId)) return
        viewModelScope.launch {
            try {
                goalRepository.setStageCompleted(stageId, completed)
                emitFeedback(
                    if (completed) {
                        GoalFeedbackType.STAGE_COMPLETED
                    } else {
                        GoalFeedbackType.STAGE_REOPENED
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emitFeedback(GoalFeedbackType.STAGE_SAVE_FAILED)
            } finally {
                stageUpdatesInFlight.remove(stageId)
            }
        }
    }

    // ---- Goal status and deletion ----------------------------------------

    fun setGoalStatus(goalId: Long, status: GoalStatus) {
        val goal = _uiState.value.goalById(goalId) ?: return
        if (goal.status == status) return
        viewModelScope.launch {
            try {
                goalRepository.setStatus(goalId, status)
                emitFeedback(GoalFeedbackType.STATUS_UPDATED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emitFeedback(GoalFeedbackType.STATUS_FAILED)
            }
        }
    }

    fun requestGoalDelete(goalId: Long) {
        viewModelScope.launch {
            try {
                val info = goalRepository.deletionInfo(goalId) ?: return@launch
                _goalDeletion.value = GoalDeletionState(info)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emitFeedback(GoalFeedbackType.DELETE_FAILED)
            }
        }
    }

    fun dismissGoalDelete() {
        _goalDeletion.value = null
    }

    fun confirmGoalDelete() {
        val state = _goalDeletion.value ?: return
        if (state.isDeleting) return
        _goalDeletion.value = state.copy(isDeleting = true)

        viewModelScope.launch {
            try {
                goalRepository.deleteGoal(state.info.goalId)
                _goalDeletion.value = null
                emitFeedback(GoalFeedbackType.DELETED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _goalDeletion.value = state.copy(isDeleting = false)
                emitFeedback(GoalFeedbackType.DELETE_FAILED)
            }
        }
    }

    fun consumeFeedback(id: Long) {
        _uiState.update { state ->
            if (state.feedback?.id == id) state.copy(feedback = null) else state
        }
    }

    private fun emitFeedback(type: GoalFeedbackType) {
        feedbackCounter += 1
        _uiState.update { it.copy(feedback = GoalFeedback(feedbackCounter, type)) }
    }
}
