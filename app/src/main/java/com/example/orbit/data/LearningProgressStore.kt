package com.example.orbit.data

import android.content.Context
import androidx.core.content.edit
import com.example.orbit.account.AccountStore
import com.example.orbit.learning.LearningProgress
import com.example.orbit.learning.WordStudyPlanner
import com.example.orbit.learning.WordStudyProgress
import com.example.orbit.learning.WordStudyRecord
import com.google.gson.Gson

/**
 * Per-account local learning choices. The books are offline content, so their
 * daily picks and bookmarks stay on this device instead of entering the cloud
 * snapshot. Keys are namespaced by the account id: signing into another account
 * never sees this account's picks.
 *
 * Word-study history is stored as one JSON blob per account because it grows
 * with the 3,877-word book and does not fit comfortably into preference keys.
 */
class LearningProgressStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val gson = Gson()

    fun load(ownerId: Long): LearningProgress {
        val owner = ownerKey(ownerId)
        return LearningProgress(
            dailyPoemDay = preferences.getLong(key(KEY_POEM_DAY, owner), 0L),
            dailyPoemId = preferences.getString(key(KEY_POEM_ID, owner), null),
            dailyWordDay = preferences.getLong(key(KEY_WORD_DAY, owner), 0L),
            dailyWordId = preferences.getString(key(KEY_WORD_ID, owner), null),
            bookmarkedWordIds = preferences.getStringSet(key(KEY_BOOKMARKS, owner), emptySet())
                ?.toSet()
                .orEmpty(),
        )
    }

    fun savePoemPick(ownerId: Long, epochDay: Long, poemId: String) {
        val owner = ownerKey(ownerId)
        preferences.edit {
            putLong(key(KEY_POEM_DAY, owner), epochDay)
            putString(key(KEY_POEM_ID, owner), poemId)
        }
    }

    fun saveWordPick(ownerId: Long, epochDay: Long, wordId: String) {
        val owner = ownerKey(ownerId)
        preferences.edit {
            putLong(key(KEY_WORD_DAY, owner), epochDay)
            putString(key(KEY_WORD_ID, owner), wordId)
        }
    }

    fun toggleBookmark(ownerId: Long, wordId: String): Set<String> {
        val owner = ownerKey(ownerId)
        val bookmarks = preferences.getStringSet(key(KEY_BOOKMARKS, owner), emptySet())
            .orEmpty()
            .toMutableSet()
        if (!bookmarks.add(wordId)) {
            bookmarks.remove(wordId)
        }
        preferences.edit {
            putStringSet(key(KEY_BOOKMARKS, owner), bookmarks.toSet())
        }
        return bookmarks.toSet()
    }

    /**
     * Missing or malformed JSON falls back to a fresh history instead of
     * crashing; a partially written blob would otherwise make the learning tab
     * unusable until the app data is cleared.
     */
    fun loadWordStudy(ownerId: Long): WordStudyProgress {
        val raw = preferences.getString(key(KEY_WORD_STUDY, ownerKey(ownerId)), null)
            ?: return WordStudyProgress()
        val snapshot = runCatching {
            gson.fromJson(raw, WordStudySnapshot::class.java)
        }.getOrNull() ?: return WordStudyProgress()
        val records = snapshot.records.orEmpty()
            .filterValues { it != null }
            .mapValues { (_, record) -> requireNotNull(record) }
        val target = if (snapshot.dailyTarget == 0) {
            WordStudyPlanner.DEFAULT_TARGET
        } else {
            WordStudyPlanner.clampTarget(snapshot.dailyTarget)
        }
        return WordStudyProgress(records = records, dailyTarget = target)
    }

    fun saveWordStudy(ownerId: Long, progress: WordStudyProgress) {
        val snapshot = WordStudySnapshot(
            records = progress.records,
            dailyTarget = progress.dailyTarget,
        )
        preferences.edit {
            putString(key(KEY_WORD_STUDY, ownerKey(ownerId)), gson.toJson(snapshot))
        }
    }

    fun clear(ownerId: Long) {
        val owner = ownerKey(ownerId)
        preferences.edit {
            remove(key(KEY_POEM_DAY, owner))
            remove(key(KEY_POEM_ID, owner))
            remove(key(KEY_WORD_DAY, owner))
            remove(key(KEY_WORD_ID, owner))
            remove(key(KEY_BOOKMARKS, owner))
            remove(key(KEY_WORD_STUDY, owner))
        }
    }

    private fun ownerKey(ownerId: Long): String =
        if (ownerId == AccountStore.NO_OWNER) "local" else ownerId.toString()

    private fun key(name: String, owner: String): String = "$name.$owner"

    /** Gson-friendly mirror of [WordStudyProgress]; computed values stay out. */
    private data class WordStudySnapshot(
        val records: Map<String, WordStudyRecord?>? = null,
        val dailyTarget: Int = 0,
    )

    companion object {
        const val PREFERENCES_NAME = "orbit_learning"
        private const val KEY_POEM_DAY = "poem_day"
        private const val KEY_POEM_ID = "poem_id"
        private const val KEY_WORD_DAY = "word_day"
        private const val KEY_WORD_ID = "word_id"
        private const val KEY_BOOKMARKS = "word_bookmarks"
        private const val KEY_WORD_STUDY = "word_study"
    }
}
