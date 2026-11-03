package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.ai.AiFailure
import com.example.orbit.ai.AiFailureKind
import com.example.orbit.ai.AiRepository
import com.example.orbit.ai.AiReview
import com.example.orbit.ai.AiReviewCache
import com.example.orbit.ai.AiReviewMetrics
import com.example.orbit.ai.AiReviewRequest
import com.example.orbit.ai.AiServiceException
import com.example.orbit.ai.CachedAiReview
import com.example.orbit.data.GoalRepository
import com.example.orbit.data.GrowthRepository
import com.example.orbit.data.HabitRepository
import com.example.orbit.data.TaskRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.GrowthEvent
import com.example.orbit.model.GrowthRules
import com.example.orbit.model.Habit
import com.example.orbit.model.HabitRecord
import com.example.orbit.model.Task
import com.example.orbit.model.TaskDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiReviewUiState(
    val hasData: Boolean = false,
    val weekStartEpochDay: Long = 0L,
    val cached: CachedAiReview? = null,
    val generating: Boolean = false,
    val statusLoading: Boolean = false,
    val configured: Boolean = false,
    val configuredKnown: Boolean = false,
    val reviewRemaining: Int = 0,
    val qaBlocked: Boolean = false,
    val failure: AiFailure? = null,
) {
    val review: AiReview? get() = cached?.review
    val hasReview: Boolean get() = cached != null
    val canGenerate: Boolean
        get() = hasData &&
            !generating &&
            !qaBlocked &&
            (!configuredKnown || configured) &&
            (!configuredKnown || reviewRemaining > 0)
}

/**
 * Owns the weekly review: it aggregates local rows into a privacy-limited
 * request, calls the cloud gateway, and caches the result per account and week.
 * It never uploads task or habit text.
 */
class AiReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OrbitAccountGraph.store
    private val repository = AiRepository()
    private val cache = AiReviewCache(application)
    private val database = OrbitDatabase.getInstance(application)

    private val taskRepository = TaskRepository(database)
    private val habitRepository = HabitRepository(database)
    private val goalRepository = GoalRepository(database)
    private val growthRepository = GrowthRepository(database)

    private val _uiState = MutableStateFlow(AiReviewUiState())
    val uiState: StateFlow<AiReviewUiState> = _uiState.asStateFlow()

    private var currentRequest: AiReviewRequest? = null

    init {
        _uiState.update { it.copy(qaBlocked = store.syncBlocked) }
        observeLocalData()
        refreshStatus()
    }

    fun refreshStatus() {
        if (store.syncBlocked) {
            _uiState.update {
                it.copy(
                    qaBlocked = true,
                    statusLoading = false,
                    configured = false,
                    configuredKnown = true,
                )
            }
            return
        }
        val token = store.token
        if (token == null) {
            _uiState.update {
                it.copy(
                    statusLoading = false,
                    failure = AiFailure(AiFailureKind.AUTH),
                )
            }
            return
        }

        _uiState.update { it.copy(statusLoading = !it.configuredKnown) }
        viewModelScope.launch {
            try {
                val status = repository.status(token)
                _uiState.update {
                    it.copy(
                        statusLoading = false,
                        configured = status.configured,
                        configuredKnown = true,
                        reviewRemaining = status.reviewRemaining,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: AiServiceException) {
                _uiState.update {
                    it.copy(
                        statusLoading = false,
                        failure = exception.failure,
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        statusLoading = false,
                        failure = AiFailure(AiFailureKind.UNKNOWN),
                    )
                }
            }
        }
    }

    fun generate() {
        val state = _uiState.value
        if (state.generating || state.qaBlocked) return
        val request = currentRequest
        if (request == null || !request.hasData) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.NO_DATA)) }
            return
        }
        val token = store.token
        val userId = store.userId
        if (token == null || userId == null) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.AUTH)) }
            return
        }
        if (state.configuredKnown && !state.configured) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.NOT_CONFIGURED)) }
            return
        }
        if (state.reviewRemaining <= 0 && state.configuredKnown) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.QUOTA_EXCEEDED)) }
            return
        }

        val weekStart = request.weekStartEpochDay
        _uiState.update { it.copy(generating = true, failure = null) }
        viewModelScope.launch {
            try {
                val response = repository.generateReview(token, request)
                val review = requireNotNull(response.review)
                val cached = CachedAiReview(
                    requestId = response.requestId,
                    generatedAt = response.generatedAt,
                    promptVersion = response.promptVersion,
                    review = review,
                )
                cache.save(userId, weekStart, cached)
                _uiState.update {
                    it.copy(
                        generating = false,
                        cached = cached,
                        failure = null,
                        reviewRemaining = (it.reviewRemaining - 1).coerceAtLeast(0),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: AiServiceException) {
                _uiState.update {
                    it.copy(generating = false, failure = exception.failure)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(generating = false, failure = AiFailure(AiFailureKind.UNKNOWN))
                }
            } finally {
                refreshStatusQuietly()
            }
        }
    }

    fun dismissFailure() {
        _uiState.update { it.copy(failure = null) }
    }

    private fun observeLocalData() {
        val tasks = taskRepository.observeTasks()
        val habits = habitRepository.observeHabits()
        val records = habitRepository.observeRecords()
        val goals = goalRepository.observeGoals()
        val stages = goalRepository.observeStages()
        val events = growthRepository.observeEvents()

        viewModelScope.launch {
            combine(
                combine(tasks, habits, records) { t, h, r -> Triple(t, h, r) },
                combine(goals, stages) { g, s -> g to s },
                events,
            ) { (taskList, habitList, recordList), (goalList, stageList), growthEvents ->
                buildRequest(taskList, habitList, recordList, goalList, stageList, growthEvents)
            }
                .catch {
                    _uiState.update { it.copy(hasData = false) }
                }
                .collect { request ->
                    currentRequest = request
                    _uiState.update {
                        it.copy(
                            hasData = request.hasData,
                            weekStartEpochDay = request.weekStartEpochDay,
                        )
                    }
                    refreshCache()
                }
        }
    }

    private fun buildRequest(
        tasks: List<Task>,
        habits: List<Habit>,
        records: List<HabitRecord>,
        goals: List<com.example.orbit.model.Goal>,
        stages: List<com.example.orbit.model.GoalStage>,
        events: List<GrowthEvent>,
    ): AiReviewRequest {
        val today = TaskDate.todayEpochDay()
        val summary = GrowthRules.summary(events, today)
        return AiReviewMetrics.build(
            tasks = tasks,
            habits = habits,
            records = records,
            goals = goals,
            stages = stages,
            thisWeekXp = summary.thisWeekXp,
            today = today,
        )
    }

    private fun refreshCache() {
        val userId = store.userId
        val request = currentRequest
        if (userId == null || request == null) {
            _uiState.update { it.copy(cached = null) }
            return
        }
        val cached = cache.load(userId, request.weekStartEpochDay)
        _uiState.update { it.copy(cached = cached) }
    }

    private fun refreshStatusQuietly() {
        if (store.syncBlocked) return
        val token = store.token ?: return
        viewModelScope.launch {
            try {
                val status = repository.status(token)
                _uiState.update {
                    it.copy(
                        configured = status.configured,
                        configuredKnown = true,
                        reviewRemaining = status.reviewRemaining,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The generation failure is already on screen.
            }
        }
    }
}
