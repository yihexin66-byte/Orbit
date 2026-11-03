package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.ai.AiFailure
import com.example.orbit.ai.AiFailureKind
import com.example.orbit.ai.AiPlan
import com.example.orbit.ai.AiPlanImport
import com.example.orbit.ai.AiPlanRequest
import com.example.orbit.ai.AiPlanSelection
import com.example.orbit.ai.AiRepository
import com.example.orbit.ai.AiServiceException
import com.example.orbit.ai.AiStatus
import com.example.orbit.data.GoalRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.GoalTitleRules
import com.example.orbit.model.TaskDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AiPlanPhase {
    FORM,
    GENERATING,
    PREVIEW,
    IMPORTING,
    IMPORTED,
}

data class AiPlanFormState(
    val goal: String = "",
    val targetDate: Long = TaskDate.todayEpochDay() + DEFAULT_TARGET_DAYS,
    val dailyMinutes: Int = DEFAULT_DAILY_MINUTES,
    val currentLevel: String = "",
    val weakPoints: String = "",
    val goalError: Boolean = false,
) {
    companion object {
        const val MAX_GOAL_LENGTH = GoalTitleRules.MAX_LENGTH
        const val MAX_PROFILE_LENGTH = 200
        const val DEFAULT_TARGET_DAYS = 56L
        const val DEFAULT_DAILY_MINUTES = 45
        const val MIN_DAILY_MINUTES = 10
        const val MAX_DAILY_MINUTES = 480
        const val MAX_TARGET_DAYS = 730L
        val DAILY_MINUTE_OPTIONS = listOf(30, 45, 60, 90, 120)
    }
}

data class AiPlanUiState(
    val phase: AiPlanPhase = AiPlanPhase.FORM,
    val form: AiPlanFormState = AiPlanFormState(),
    val status: AiStatus? = null,
    val statusLoading: Boolean = false,
    val statusError: AiFailure? = null,
    val qaBlocked: Boolean = false,
    val plan: AiPlan? = null,
    val excluded: Set<String> = emptySet(),
    val failure: AiFailure? = null,
    val importError: Boolean = false,
    val emptySelection: Boolean = false,
    val importedGoalId: Long? = null,
) {
    val configured: Boolean get() = status?.configured == true
    val planRemaining: Int get() = status?.planRemaining ?: 0
    val planTaskCount: Int get() = plan?.taskCount ?: 0
    val selectedTaskCount: Int
        get() = plan?.let { AiPlanSelection.selectedTaskCount(it, excluded) } ?: 0
    val isBusy: Boolean
        get() = phase == AiPlanPhase.GENERATING || phase == AiPlanPhase.IMPORTING
    val canGenerate: Boolean
        get() = phase == AiPlanPhase.FORM &&
            !qaBlocked &&
            form.goal.isNotBlank() &&
            status?.configured == true &&
            planRemaining > 0
}

/**
 * Orchestrates the AI planning flow: quota status, one generation attempt,
 * review selection and an atomic import. The provider token never reaches the
 * app; all AI traffic goes through the signed-in cloud gateway.
 */
class AiPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OrbitAccountGraph.store
    private val repository = AiRepository()
    private val goalRepository = GoalRepository(OrbitDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(AiPlanUiState())
    val uiState: StateFlow<AiPlanUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val blocked = store.syncBlocked
        if (blocked) {
            _uiState.update {
                it.copy(qaBlocked = true, statusLoading = false, statusError = null)
            }
            return
        }
        val token = store.token
        if (token == null) {
            _uiState.update {
                it.copy(
                    statusLoading = false,
                    statusError = AiFailure(AiFailureKind.AUTH),
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(statusLoading = state.status == null, statusError = null)
        }
        viewModelScope.launch {
            try {
                val status = repository.status(token)
                _uiState.update {
                    it.copy(status = status, statusLoading = false, statusError = null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: AiServiceException) {
                _uiState.update {
                    it.copy(statusLoading = false, statusError = exception.failure)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(statusLoading = false, statusError = AiFailure(AiFailureKind.UNKNOWN))
                }
            }
        }
    }

    fun updateGoal(value: String) {
        _uiState.update { state ->
            state.copy(
                form = state.form.copy(
                    goal = value.take(AiPlanFormState.MAX_GOAL_LENGTH),
                    goalError = false,
                ),
                failure = null,
            )
        }
    }

    fun updateTargetDate(epochDay: Long) {
        val today = TaskDate.todayEpochDay()
        val clamped = epochDay.coerceIn(today, today + AiPlanFormState.MAX_TARGET_DAYS)
        _uiState.update { it.copy(form = it.form.copy(targetDate = clamped)) }
    }

    fun updateDailyMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(
            AiPlanFormState.MIN_DAILY_MINUTES,
            AiPlanFormState.MAX_DAILY_MINUTES,
        )
        _uiState.update { it.copy(form = it.form.copy(dailyMinutes = clamped)) }
    }

    fun updateCurrentLevel(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(currentLevel = value.take(AiPlanFormState.MAX_PROFILE_LENGTH)))
        }
    }

    fun updateWeakPoints(value: String) {
        _uiState.update {
            it.copy(form = it.form.copy(weakPoints = value.take(AiPlanFormState.MAX_PROFILE_LENGTH)))
        }
    }

    fun generate() {
        val state = _uiState.value
        if (state.isBusy) return
        if (state.qaBlocked) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.NOT_CONFIGURED)) }
            return
        }
        if (!GoalTitleRules.isValid(state.form.goal)) {
            _uiState.update { it.copy(form = it.form.copy(goalError = true)) }
            return
        }
        val planStillVisible = state.phase == AiPlanPhase.PREVIEW && state.plan != null
        val token = store.token
        if (token == null) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.AUTH)) }
            return
        }
        val status = state.status
        when {
            status != null && !status.configured -> {
                _uiState.update { it.copy(failure = AiFailure(AiFailureKind.NOT_CONFIGURED)) }
                return
            }

            status != null && status.planRemaining <= 0 -> {
                _uiState.update { it.copy(failure = AiFailure(AiFailureKind.QUOTA_EXCEEDED)) }
                return
            }
        }

        _uiState.update {
            it.copy(
                phase = AiPlanPhase.GENERATING,
                failure = null,
                importError = false,
                emptySelection = false,
            )
        }
        viewModelScope.launch {
            try {
                val response = repository.generatePlan(
                    token = token,
                    request = AiPlanRequest(
                        goal = state.form.goal.trim(),
                        targetDateEpochDay = state.form.targetDate,
                        dailyMinutes = state.form.dailyMinutes,
                        currentLevel = state.form.currentLevel.trim(),
                        weakPoints = state.form.weakPoints.trim(),
                    ),
                )
                val plan = requireNotNull(response.plan)
                _uiState.update {
                    it.copy(
                        phase = AiPlanPhase.PREVIEW,
                        plan = plan,
                        excluded = emptySet(),
                        failure = null,
                        emptySelection = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: AiServiceException) {
                _uiState.update {
                    it.copy(
                        phase = if (planStillVisible) AiPlanPhase.PREVIEW else AiPlanPhase.FORM,
                        failure = exception.failure,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        phase = if (planStillVisible) AiPlanPhase.PREVIEW else AiPlanPhase.FORM,
                        failure = AiFailure(AiFailureKind.UNKNOWN),
                    )
                }
            } finally {
                refreshQuietly()
            }
        }
    }

    fun toggleTask(stageIndex: Int, taskIndex: Int) {
        _uiState.update { state ->
            state.copy(
                excluded = AiPlanSelection.toggleTask(
                    state.excluded,
                    stageIndex,
                    taskIndex,
                ),
                emptySelection = false,
            )
        }
    }

    fun toggleStage(stageIndex: Int) {
        _uiState.update { state ->
            val plan = state.plan ?: return@update state
            state.copy(
                excluded = AiPlanSelection.toggleStage(plan, state.excluded, stageIndex),
                emptySelection = false,
            )
        }
    }

    fun importSelected() {
        val state = _uiState.value
        if (state.phase != AiPlanPhase.PREVIEW) return
        val plan = state.plan ?: return
        val selected = AiPlanSelection.selectedPlan(plan, state.excluded)
        if (selected == null || selected.taskCount == 0) {
            _uiState.update { it.copy(emptySelection = true) }
            return
        }
        val importPlan = AiPlanImport.toImportPlan(
            plan = selected,
            fallbackTitle = state.form.goal,
            today = TaskDate.todayEpochDay(),
        )
        if (importPlan == null) {
            _uiState.update { it.copy(importError = true) }
            return
        }

        _uiState.update {
            it.copy(
                phase = AiPlanPhase.IMPORTING,
                importError = false,
                emptySelection = false,
            )
        }
        viewModelScope.launch {
            try {
                val goalId = goalRepository.importPlan(importPlan)
                _uiState.update {
                    it.copy(phase = AiPlanPhase.IMPORTED, importedGoalId = goalId)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(phase = AiPlanPhase.PREVIEW, importError = true)
                }
            }
        }
    }

    fun discardPlan() {
        if (_uiState.value.isBusy) return
        _uiState.update {
            it.copy(
                phase = AiPlanPhase.FORM,
                plan = null,
                excluded = emptySet(),
                failure = null,
                importError = false,
                emptySelection = false,
            )
        }
    }

    fun dismissFailure() {
        _uiState.update { it.copy(failure = null) }
    }

    fun consumeImportedGoal() {
        _uiState.update { it.copy(importedGoalId = null) }
    }

    private fun refreshQuietly() {
        if (store.syncBlocked) return
        val token = store.token ?: return
        viewModelScope.launch {
            try {
                val status = repository.status(token)
                _uiState.update { it.copy(status = status, statusError = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The attempt already surfaced its own failure; quota refresh is best effort.
            }
        }
    }
}
