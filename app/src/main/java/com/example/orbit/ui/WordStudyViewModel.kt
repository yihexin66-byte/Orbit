package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.account.AccountStore
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.data.LearningProgressStore
import com.example.orbit.learning.LearningLibrary
import com.example.orbit.learning.WordStudyPlanner
import com.example.orbit.learning.WordStudyProgress
import com.example.orbit.learning.WordStudySession
import com.example.orbit.model.TaskDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordStudyState(
    val progress: WordStudyProgress = WordStudyProgress(),
    val session: WordStudySession? = null,
)

data class WordStudyActions(
    val onSetTarget: (Int) -> Unit = {},
    val onStartAuto: () -> Unit = {},
    val onStartManual: (Set<String>) -> Unit = {},
    val onReveal: () -> Unit = {},
    val onAnswer: (Boolean) -> Unit = {},
    val onRetryWrong: () -> Unit = {},
    val onNextRound: () -> Unit = {},
    val onFinish: () -> Unit = {},
)

/**
 * Word-study rounds over the bundled 3500-word book. A session is kept in the
 * ViewModel rather than in the sheet, so closing the sheet or switching tabs
 * still offers "继续学习" for the same round. Answers are written through on
 * every card, which also keeps a killed app from losing the round's progress.
 */
class WordStudyViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LearningProgressStore(application)
    private var ownerId = OrbitAccountGraph.store.userId ?: AccountStore.NO_OWNER

    private val _state = MutableStateFlow(
        WordStudyState(progress = store.loadWordStudy(ownerId)),
    )
    val state: StateFlow<WordStudyState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            OrbitAccountGraph.store.session.collect { session ->
                val nextOwner = session?.user?.id ?: AccountStore.NO_OWNER
                if (nextOwner != ownerId) {
                    ownerId = nextOwner
                    // A round belongs to the account that started it.
                    _state.value = WordStudyState(progress = store.loadWordStudy(nextOwner))
                }
            }
        }
    }

    fun setTarget(target: Int) {
        val progress = _state.value.progress.withTarget(target)
        persist(progress)
        _state.update { it.copy(progress = progress) }
    }

    fun startAuto() {
        val progress = _state.value.progress
        val words = WordStudyPlanner.autoSelect(
            words = LearningLibrary.english,
            progress = progress,
            count = progress.dailyTarget,
        )
        if (words.isEmpty()) return
        _state.update { it.copy(session = WordStudySession(words)) }
    }

    fun startManual(ids: Set<String>) {
        val words = WordStudyPlanner.manualSelect(LearningLibrary.english, ids)
        if (words.isEmpty()) return
        _state.update { it.copy(session = WordStudySession(words)) }
    }

    fun reveal() {
        val session = _state.value.session ?: return
        _state.update { it.copy(session = session.reveal()) }
    }

    fun answer(known: Boolean) {
        val session = _state.value.session ?: return
        val word = session.current ?: return
        if (!session.revealed) return
        val progress = _state.value.progress.record(
            wordId = word.id,
            known = known,
            epochDay = TaskDate.todayEpochDay(),
        )
        persist(progress)
        _state.update {
            it.copy(progress = progress, session = session.answer(known))
        }
    }

    fun retryWrong() {
        val retry = _state.value.session?.retryWrong() ?: return
        _state.update { it.copy(session = retry) }
    }

    fun nextRound() {
        startAuto()
    }

    /** Leaves the summary; the completed session can be revisited until then. */
    fun finish() {
        _state.update { it.copy(session = null) }
    }

    fun reload() {
        _state.value = WordStudyState(progress = store.loadWordStudy(ownerId))
    }

    private fun persist(progress: WordStudyProgress) {
        store.saveWordStudy(ownerId, progress)
    }
}
