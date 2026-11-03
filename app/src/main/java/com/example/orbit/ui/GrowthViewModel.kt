package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.data.GrowthRepository
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.GrowthEvent
import com.example.orbit.model.GrowthRules
import com.example.orbit.model.GrowthSummary
import com.example.orbit.model.TaskDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GrowthLoadStatus {
    LOADING,
    READY,
    FAILED,
}

data class GrowthUiState(
    val events: List<GrowthEvent> = emptyList(),
    val loadStatus: GrowthLoadStatus = GrowthLoadStatus.LOADING,
    val todayEpochDay: Long = TaskDate.todayEpochDay(),
) {
    val isLoading: Boolean get() = loadStatus == GrowthLoadStatus.LOADING
    val loadFailed: Boolean get() = loadStatus == GrowthLoadStatus.FAILED
    val hasEvents: Boolean get() = events.isNotEmpty()

    val summary: GrowthSummary get() = GrowthRules.summary(events, todayEpochDay)
}

/**
 * Read-only projection of the growth ledger. Writes stay in the repositories
 * that own their source so XP cannot be granted from the UI layer.
 */
class GrowthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GrowthRepository(OrbitDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(GrowthUiState())
    val uiState: StateFlow<GrowthUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeEvents()
    }

    fun retryLoad() {
        observeEvents()
    }

    fun refreshToday(nowMillis: Long = System.currentTimeMillis()) {
        val today = TaskDate.todayEpochDay(nowMillis)
        _uiState.update { state ->
            if (state.todayEpochDay == today) state else state.copy(todayEpochDay = today)
        }
    }

    private fun observeEvents() {
        observeJob?.cancel()
        _uiState.update {
            it.copy(
                loadStatus = GrowthLoadStatus.LOADING,
                todayEpochDay = TaskDate.todayEpochDay(),
            )
        }

        observeJob = viewModelScope.launch {
            repository.observeEvents()
                .catch {
                    _uiState.update { state -> state.copy(loadStatus = GrowthLoadStatus.FAILED) }
                }
                .collect { events ->
                    _uiState.update { state ->
                        state.copy(
                            events = events,
                            loadStatus = GrowthLoadStatus.READY,
                            todayEpochDay = TaskDate.todayEpochDay(),
                        )
                    }
                }
        }
    }
}
