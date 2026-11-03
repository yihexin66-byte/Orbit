package com.example.orbit.ai

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson

/**
 * Weekly reviews are AI-generated text, not source data. Caching them in
 * SharedPreferences keeps Room at v2 and still gives each account its own copy
 * per calendar week. A new week naturally misses the cache.
 */
class AiReviewCache(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val gson = Gson()

    fun load(userId: Long, weekStartEpochDay: Long): CachedAiReview? {
        val json = preferences.getString(key(userId, weekStartEpochDay), null) ?: return null
        return runCatching { gson.fromJson(json, CachedAiReview::class.java) }.getOrNull()
    }

    fun save(userId: Long, weekStartEpochDay: Long, review: CachedAiReview) {
        preferences.edit {
            putString(key(userId, weekStartEpochDay), gson.toJson(review))
        }
    }

    fun clearUser(userId: Long) {
        val prefix = "review_${userId}_"
        val stale = preferences.all.keys.filter { it.startsWith(prefix) }
        if (stale.isEmpty()) return
        preferences.edit {
            stale.forEach(::remove)
        }
    }

    private fun key(userId: Long, weekStartEpochDay: Long): String =
        "review_${userId}_$weekStartEpochDay"

    companion object {
        const val PREFERENCES_NAME = "orbit_ai_review"
    }
}
