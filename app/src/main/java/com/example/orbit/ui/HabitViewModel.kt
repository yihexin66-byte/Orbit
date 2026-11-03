package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.data.HabitRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.AccentTone
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitMetrics
import com.example.orbit.model.HabitRecord
import com.example.orbit.model.HabitSummary
import com.example.orbit.model.HabitTitleRules
import com.example.orbit.model.TaskDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HabitLoadStatus {
    LOADING,
    READY,
    FAILED,
}

data class HabitUiState(
    val habits: List<Habit> = emptyList(),
    val records: List<HabitRecord> = emptyList(),
    val loadStatus: HabitLoadStatus = HabitLoadStatus.LOADING,
    /** One snapshot of the day feeds every streak, heatmap and check state. */
    val todayEpochDay: Long = TaskDate.todayEpochDay(),
    val feedback: HabitFeedback? = null,
) {
    val isLoading: Boolean get() = loadStatus == HabitLoadStatus.LOADING
    val loadFailed: Boolean get() = loadStatus == HabitLoadStatus.FAILED

    val activeHabits: List<Habit>
        get() = habits
            .filterNot { it.isArchived }
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))

    val archivedHabits: List<Habit>
        get() = habits
            .filter { it.isArchived }
            .sortedByDescending { it.archivedAt ?: 0L }

    val activeCount: Int get() = activeHabits.size

    val checkedTodayCount: Int
        get() = activeHabits.count { isCheckedToday(it.id) }

    val todayProgress: Float
        get() = if (activeCount == 0) 0f else checkedTodayCount.toFloat() / activeCount

    val todayPendingCount: Int get() = (activeCount - checkedTodayCount).coerceAtLeast(0)

    fun isCheckedToday(habitId: Long): Boolean = records.any {
        it.habitId == habitId && it.epochDay == todayEpochDay
    }

    fun summaryFor(habit: Habit): HabitSummary =
        HabitMetrics.summary(habit, records, todayEpochDay)
}

data class HabitFeedback(
    val id: Long,
    val type: HabitFeedbackType,
)

enum class HabitFeedbackType {
    CREATED,
    UPDATED,
    ARCHIVED,
    RESTORED,
    DELETED,
    CHECKED,
    UNCHECKED,
    SAVE_FAILED,
    DELETE_FAILED,
    ARCHIVE_FAILED,
    TOGGLE_FAILED,
}

enum class HabitEditorActionError {
    SAVE_FAILED,
    DELETE_FAILED,
    ARCHIVE_FAILED,
}

data class HabitEditorState(
    val habitId: Long? = null,
    val title: String = "",
    val note: String = "",
    val tone: AccentTone = AccentTone.VIOLET,
    val isArchived: Boolean = false,
    val titleError: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val actionError: HabitEditorActionError? = null,
    val showDeleteConfirm: Boolean = false,
) {
    val isEditing: Boolean get() = habitId != null

    companion object {
        const val MAX_TITLE_LENGTH = HabitTitleRules.MAX_LENGTH
        const val MAX_NOTE_LENGTH = 160
    }
}

class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HabitRepository(OrbitDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    private val _editorState = MutableStateFlow<HabitEditorState?>(null)
    val editorState: StateFlow<HabitEditorState?> = _editorState.asStateFlow()

    private var observeJob: Job? = null
    private var feedbackCounter = 0L
    private val checkUpdatesInFlight = mutableSetOf<Long>()
    private val archiveUpdatesInFlight = mutableSetOf<Long>()

    init {
        observeHabits()
    }

    fun retryLoad() {
        observeHabits()
    }

    /** Called when the app returns to the foreground so overnight sessions roll over. */
    fun refreshToday(nowMillis: Long = System.currentTimeMillis()) {
        val today = TaskDate.todayEpochDay(nowMillis)
        _uiState.update { state ->
            if (state.todayEpochDay == today) state else state.copy(todayEpochDay = today)
        }
    }

    private fun observeHabits() {
        observeJob?.cancel()
        _uiState.update {
            it.copy(
                loadStatus = HabitLoadStatus.LOADING,
                todayEpochDay = TaskDate.todayEpochDay(),
            )
        }

        observeJob = viewModelScope.launch {
            combine(
                repository.observeHabits(),
                repository.observeRecords(),
            ) { habits, records -> habits to records }
                .catch {
                    _uiState.update { state ->
                        state.copy(loadStatus = HabitLoadStatus.FAILED)
                    }
                }
                .collect { (habits, records) ->
                    _uiState.update { state ->
                        state.copy(
                            habits = habits,
                            records = records,
                            loadStatus = HabitLoadStatus.READY,
                            todayEpochDay = TaskDate.todayEpochDay(),
                        )
                    }
                }
        }
    }

    fun toggleChecked(habitId: Long) {
        refreshToday()
        val state = _uiState.value
        val habit = state.habits.firstOrNull { it.id == habitId } ?: return
        if (habit.isArchived) return
        if (!checkUpdatesInFlight.add(habitId)) return

        val today = state.todayEpochDay
        val checking = !state.isCheckedToday(habitId)
        viewModelScope.launch {
            try {
                repository.setChecked(habitId, today, checking)
                emitFeedback(
                    if (checking) HabitFeedbackType.CHECKED else HabitFeedbackType.UNCHECKED,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emitFeedback(HabitFeedbackType.TOGGLE_FAILED)
            } finally {
                checkUpdatesInFlight.remove(habitId)
            }
        }
    }

    fun openCreateHabit() {
        _editorState.value = HabitEditorState()
    }

    fun openEditHabit(habitId: Long) {
        val habit = _uiState.value.habits.firstOrNull { it.id == habitId } ?: return
        _editorState.value = HabitEditorState(
            habitId = habit.id,
            title = habit.title,
            note = habit.note.orEmpty(),
            tone = habit.tone,
            isArchived = habit.isArchived,
        )
    }

    fun dismissEditor() {
        _editorState.value = null
    }

    fun updateEditorTitle(title: String) {
        _editorState.update { editor ->
            editor?.copy(
                title = title.take(HabitEditorState.MAX_TITLE_LENGTH),
                titleError = false,
                actionError = null,
            )
        }
    }

    fun updateEditorNote(note: String) {
        _editorState.update { editor ->
            editor?.copy(
                note = note.take(HabitEditorState.MAX_NOTE_LENGTH),
                actionError = null,
            )
        }
    }

    fun updateEditorTone(tone: AccentTone) {
        _editorState.update { it?.copy(tone = tone) }
    }

    fun saveEditor() {
        val editor = _editorState.value ?: return
        if (editor.isSaving || editor.isDeleting) return

        if (!HabitTitleRules.isValid(editor.title)) {
            _editorState.update { it?.copy(titleError = true) }
            return
        }

        _editorState.update {
            it?.copy(isSaving = true, titleError = false, actionError = null)
        }

        viewModelScope.launch {
            try {
                if (editor.isEditing) {
                    repository.updateHabit(
                        id = requireNotNull(editor.habitId),
                        title = editor.title,
                        note = editor.note,
                        tone = editor.tone,
                    )
                } else {
                    repository.createHabit(
                        title = editor.title,
                        note = editor.note,
                        tone = editor.tone,
                    )
                }
                _editorState.value = null
                emitFeedback(
                    if (editor.isEditing) HabitFeedbackType.UPDATED else HabitFeedbackType.CREATED,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _editorState.update {
                    it?.copy(
                        isSaving = false,
                        actionError = HabitEditorActionError.SAVE_FAILED,
                    )
                }
                emitFeedback(HabitFeedbackType.SAVE_FAILED)
            }
        }
    }

    /**
     * Archiving keeps the check-in history; the editor closes after the change
     * so the message can confirm that nothing was lost.
     */
    fun setArchived(habitId: Long, archived: Boolean) {
        if (!archiveUpdatesInFlight.add(habitId)) return

        viewModelScope.launch {
            try {
                repository.setArchived(habitId, archived)
                if (_editorState.value?.habitId == habitId) {
                    _editorState.value = null
                }
                emitFeedback(
                    if (archived) HabitFeedbackType.ARCHIVED else HabitFeedbackType.RESTORED,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _editorState.update { editor ->
                    if (editor?.habitId == habitId) {
                        editor.copy(actionError = HabitEditorActionError.ARCHIVE_FAILED)
                    } else {
                        editor
                    }
                }
                emitFeedback(HabitFeedbackType.ARCHIVE_FAILED)
            } finally {
                archiveUpdatesInFlight.remove(habitId)
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
        val habitId = editor.habitId ?: return
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
                repository.deleteHabit(habitId)
                _editorState.value = null
                emitFeedback(HabitFeedbackType.DELETED)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _editorState.update {
                    it?.copy(
                        isDeleting = false,
                        actionError = HabitEditorActionError.DELETE_FAILED,
                    )
                }
                emitFeedback(HabitFeedbackType.DELETE_FAILED)
            }
        }
    }

    fun consumeFeedback(id: Long) {
        _uiState.update { state ->
            if (state.feedback?.id == id) state.copy(feedback = null) else state
        }
    }

    private fun emitFeedback(type: HabitFeedbackType) {
        feedbackCounter += 1
        _uiState.update { it.copy(feedback = HabitFeedback(feedbackCounter, type)) }
    }
}
