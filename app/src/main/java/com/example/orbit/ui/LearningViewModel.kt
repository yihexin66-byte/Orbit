package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.account.AccountStore
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.data.LearningProgressStore
import com.example.orbit.learning.LearningProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearningActions(
    val onSetPoemToday: (Long, String) -> Unit = { _, _ -> },
    val onSetWordToday: (Long, String) -> Unit = { _, _ -> },
    val onToggleWordBookmark: (String) -> Unit = {},
)

/**
 * Daily poem/word picks and word bookmarks. These are local to this device and
 * namespaced by account, unlike tasks and habits which live in Room + cloud.
 */
class LearningViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LearningProgressStore(application)
    private var ownerId = OrbitAccountGraph.store.userId ?: AccountStore.NO_OWNER

    private val _progress = MutableStateFlow(store.load(ownerId))
    val progress: StateFlow<LearningProgress> = _progress.asStateFlow()

    init {
        viewModelScope.launch {
            OrbitAccountGraph.store.session.collect { session ->
                val nextOwner = session?.user?.id ?: AccountStore.NO_OWNER
                if (nextOwner != ownerId) {
                    ownerId = nextOwner
                    _progress.value = store.load(nextOwner)
                }
            }
        }
    }

    fun setPoemToday(epochDay: Long, poemId: String) {
        store.savePoemPick(ownerId, epochDay, poemId)
        _progress.update {
            it.copy(dailyPoemDay = epochDay, dailyPoemId = poemId)
        }
    }

    fun setWordToday(epochDay: Long, wordId: String) {
        store.saveWordPick(ownerId, epochDay, wordId)
        _progress.update {
            it.copy(dailyWordDay = epochDay, dailyWordId = wordId)
        }
    }

    fun toggleWordBookmark(wordId: String) {
        val bookmarks = store.toggleBookmark(ownerId, wordId)
        _progress.update { it.copy(bookmarkedWordIds = bookmarks) }
    }

    /** Re-reads preferences, e.g. after a test or a process-level reset. */
    fun reload() {
        _progress.value = store.load(ownerId)
    }

    /** Test/debug reset for the current account. */
    fun resetLocalProgress() {
        store.clear(ownerId)
        _progress.value = store.load(ownerId)
    }
}
