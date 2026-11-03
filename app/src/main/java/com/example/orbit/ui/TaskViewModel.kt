package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.data.TaskRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.Task
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskDate
import com.example.orbit.model.TaskFilter
import com.example.orbit.model.TaskMetrics
import com.example.orbit.model.TaskPriority
import com.example.orbit.model.TaskStatistics
import com.example.orbit.model.TaskSummary
import com.example.orbit.model.TaskTitleRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskLoadStatus {
    LOADING,
    READY,
    FAILED,
}

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val selectedTaskId: Long? = null,
    val loadStatus: TaskLoadStatus = TaskLoadStatus.LOADING,
    /** Snapshot used by every derived metric so one recomposition uses one day. */
    val todayEpochDay: Long = TaskDate.todayEpochDay(),
    val feedback: TaskFeedback? = null,
) {
    val isLoading: Boolean get() = loadStatus == TaskLoadStatus.LOADING
    val loadFailed: Boolean get() = loadStatus == TaskLoadStatus.FAILED

    val todayTasks: List<Task>
        get() = TaskMetrics.todayTasks(tasks, todayEpochDay)

    val todaySummary: TaskSummary
        get() = TaskMetrics.todaySummary(tasks, todayEpochDay)

    val statistics: TaskStatistics
        get() = TaskMetrics.statistics(tasks, todayEpochDay)

    val todayPlanned: Int get() = todaySummary.planned

    val todayCompleted: Int get() = todaySummary.completed

    val todayPending: Int get() = todaySummary.pending

    val todayProgress: Float get() = todaySummary.progress

    val pendingCount: Int
        get() = tasks.count { !it.isCompleted }

    val completedCount: Int
        get() = tasks.count { it.isCompleted }

    val selectedTask: Task?
        get() = todayTasks.firstOrNull { it.id == selectedTaskId }

    val filteredTasks: List<Task>
        get() = TaskMetrics.tasksFor(tasks, filter)

    fun countFor(filter: TaskFilter): Int = TaskMetrics.countFor(tasks, filter)
}

data class TaskFeedback(
    val id: Long,
    val type: TaskFeedbackType,
)

enum class TaskFeedbackType {
    CREATED,
    UPDATED,
    DELETED,
    COMPLETED,
    REOPENED,
    SAVE_FAILED,
    DELETE_FAILED,
    TOGGLE_FAILED,
}

enum class TaskEditorActionError {
    SAVE_FAILED,
    DELETE_FAILED,
}

data class TaskEditorState(
    val taskId: Long? = null,
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.OTHER,
    val dueDate: Long? = null,
    val goalId: Long? = null,
    val stageId: Long? = null,
    val titleError: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val actionError: TaskEditorActionError? = null,
    val showDeleteConfirm: Boolean = false,
) {
    val isEditing: Boolean get() = taskId != null

    companion object {
        const val MAX_TITLE_LENGTH = TaskTitleRules.MAX_LENGTH
        const val MAX_DESCRIPTION_LENGTH = 280
    }
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(OrbitDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _editorState = MutableStateFlow<TaskEditorState?>(null)
    val editorState: StateFlow<TaskEditorState?> = _editorState.asStateFlow()

    private var observeJob: Job? = null
    private var feedbackCounter = 0L
    private val completionUpdatesInFlight = mutableSetOf<Long>()

    init {
        observeTasks()
    }

    fun retryLoad() {
        observeTasks()
    }

    /** Called when the app returns to the foreground so overnight sessions roll over. */
    fun refreshToday(nowMillis: Long = System.currentTimeMillis()) {
        val today = TaskDate.todayEpochDay(nowMillis)
        _uiState.update { state ->
            if (state.todayEpochDay == today) {
                state
            } else {
                state.copy(
                    todayEpochDay = today,
                    selectedTaskId = resolveSelection(state.selectedTaskId, state.tasks, today),
                )
            }
        }
    }

    private fun observeTasks() {
        observeJob?.cancel()
        _uiState.update {
            it.copy(
                loadStatus = TaskLoadStatus.LOADING,
                todayEpochDay = TaskDate.todayEpochDay(),
            )
        }

        observeJob = viewModelScope.launch {
            repository.observeTasks()
                .catch {
                    _uiState.update { state ->
                        state.copy(loadStatus = TaskLoadStatus.FAILED)
                    }
                }
                .collect { tasks ->
                    _uiState.update { state ->
                        val today = TaskDate.todayEpochDay()
                        state.copy(
                            tasks = tasks,
                            loadStatus = TaskLoadStatus.READY,
                            todayEpochDay = today,
                            selectedTaskId = resolveSelection(state.selectedTaskId, tasks, today),
                        )
                    }
                }
        }
    }

    /**
     * The orbit starts with no detail open. A selection only survives while
     * that task is still one of today's tasks; deleting it, moving its due
     * date or crossing midnight closes the detail instead of showing stale data.
     */
    private fun resolveSelection(currentId: Long?, tasks: List<Task>, today: Long): Long? =
        currentId?.takeIf { id ->
            tasks.any { task -> task.id == id && task.dueDate == today }
        }

    fun setFilter(filter: TaskFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun selectTask(taskId: Long) {
        _uiState.update { state ->
            if (state.todayTasks.any { it.id == taskId }) {
                state.copy(selectedTaskId = taskId)
            } else {
                state
            }
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            if (state.selectedTaskId == null) state else state.copy(selectedTaskId = null)
        }
    }

    fun toggleTask(taskId: Long) {
        val task = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        if (!completionUpdatesInFlight.add(task.id)) return

        val completing = !task.isCompleted
        viewModelScope.launch {
            try {
                repository.setCompleted(task.id, completing)
                emitFeedback(
                    if (completing) TaskFeedbackType.COMPLETED else TaskFeedbackType.REOPENED,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emitFeedback(TaskFeedbackType.TOGGLE_FAILED)
            } finally {
                completionUpdatesInFlight.remove(task.id)
            }
        }
    }

    fun openCreateTask(goalId: Long? = null, stageId: Long? = null) {
        // A new task should default to the real current day even after midnight.
        refreshToday()
        _editorState.value = TaskEditorState(
            dueDate = _uiState.value.todayEpochDay,
            goalId = goalId,
            stageId = if (goalId == null) null else stageId,
        )
    }

    fun openEditTask(taskId: Long) {
        val task = _uiState.value.tasks.firstOrNull { it.id == taskId } ?: return
        _editorState.value = TaskEditorState(
            taskId = task.id,
            title = task.title,
            description = task.description.orEmpty(),
            priority = task.priority,
            category = task.category,
            dueDate = task.dueDate,
            goalId = task.goalId,
            stageId = task.stageId,
        )
    }

    fun dismissEditor() {
        _editorState.value = null
    }

    fun updateEditorTitle(title: String) {
        _editorState.update { editor ->
            editor?.copy(
                title = title.take(TaskEditorState.MAX_TITLE_LENGTH),
                titleError = false,
                actionError = null,
            )
        }
    }

    fun updateEditorDescription(description: String) {
        _editorState.update { editor ->
            editor?.copy(
                description = description.take(TaskEditorState.MAX_DESCRIPTION_LENGTH),
                actionError = null,
            )
        }
    }

    fun updateEditorPriority(priority: TaskPriority) {
        _editorState.update { it?.copy(priority = priority) }
    }

    fun updateEditorCategory(category: TaskCategory) {
        _editorState.update { it?.copy(category = category) }
    }

    fun updateEditorDueDate(dueDate: Long?) {
        _editorState.update { it?.copy(dueDate = dueDate) }
    }

    /**
     * Selecting a different goal clears the stage unless the caller passes one
     * from that same goal (the editor always does).
     */
    fun updateEditorLink(goalId: Long?, stageId: Long? = null) {
        _editorState.update { editor ->
            editor?.copy(
                goalId = goalId,
                stageId = if (goalId == null) null else stageId,
            )
        }
    }

    fun saveEditor() {
        val editor = _editorState.value ?: return
        if (editor.isSaving || editor.isDeleting) return

        if (!TaskTitleRules.isValid(editor.title)) {
            _editorState.update { it?.copy(titleError = true) }
            return
        }

        _editorState.update {
            it?.copy(isSaving = true, titleError = false, actionError = null)
        }

        viewModelScope.launch {
            try {
                if (editor.isEditing) {
                    repository.updateTask(
                        id = requireNotNull(editor.taskId),
                        title = editor.title,
                        description = editor.description,
                        priority = editor.priority,
                        category = editor.category,
                        dueDate = editor.dueDate,
                        goalId = editor.goalId,
                        stageId = editor.stageId,
                    )
                } else {
                    repository.createTask(
                        title = editor.title,
                        description = editor.description,
                        priority = editor.priority,
                        category = editor.category,
                        dueDate = editor.dueDate,
                        goalId = editor.goalId,
                        stageId = editor.stageId,
                    )
                }
                _editorState.value = null
                emitFeedback(
                    if (editor.isEditing) TaskFeedbackType.UPDATED else TaskFeedbackType.CREATED,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _editorState.update {
                    it?.copy(
                        isSaving = false,
                        actionError = TaskEditorActionError.SAVE_FAILED,
                    )
                }
                emitFeedback(TaskFeedbackType.SAVE_FAILED)
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
        val taskId = editor.taskId ?: return
        if (editor.isSaving || editor.isDeleting) return

        _editorState.update {
            it?.copy(
                showDeleteConfirm = false,
                isDeleting = true,
                actionError = null,
            )
        }

        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                _editorState.value = null
                _uiState.update { state ->
                    state.copy(selectedTaskId = state.selectedTaskId.takeUnless { it == taskId })
                }
                emitFeedback(TaskFeedbackType.DELETED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _editorState.update {
                    it?.copy(
                        isDeleting = false,
                        actionError = TaskEditorActionError.DELETE_FAILED,
                    )
                }
                emitFeedback(TaskFeedbackType.DELETE_FAILED)
            }
        }
    }

    fun consumeFeedback(id: Long) {
        _uiState.update { state ->
            if (state.feedback?.id == id) state.copy(feedback = null) else state
        }
    }

    private fun emitFeedback(type: TaskFeedbackType) {
        feedbackCounter += 1
        _uiState.update { it.copy(feedback = TaskFeedback(feedbackCounter, type)) }
    }
}
