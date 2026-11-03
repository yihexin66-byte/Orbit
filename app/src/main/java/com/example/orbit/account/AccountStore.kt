package com.example.orbit.account

import android.content.Context
import androidx.core.content.edit
import com.example.orbit.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Synchronous account state. The token and profile live in SharedPreferences so
 * the first Compose frame already knows whether a session exists. The local
 * database belongs to [localOwnerUserId]; -1 means it predates any account.
 */
class AccountStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val gson = Gson()

    private val _session = MutableStateFlow(readSession())
    val session: StateFlow<StoredAccount?> = _session.asStateFlow()

    val token: String? get() = _session.value?.token
    val userId: Long? get() = _session.value?.user?.id

    var localOwnerUserId: Long
        get() = preferences.getLong(KEY_LOCAL_OWNER, NO_OWNER)
        set(value) = preferences.edit { putLong(KEY_LOCAL_OWNER, value) }

    var syncRevision: Long
        get() = preferences.getLong(KEY_SYNC_REVISION, 0L)
        set(value) = preferences.edit { putLong(KEY_SYNC_REVISION, value) }

    /** True when the local cache has changes the last successful sync did not cover. */
    var localDirty: Boolean
        get() = preferences.getBoolean(KEY_LOCAL_DIRTY, false)
        set(value) = preferences.edit { putBoolean(KEY_LOCAL_DIRTY, value) }

    var lastSyncedAt: Long
        get() = preferences.getLong(KEY_LAST_SYNC, 0L)
        set(value) = preferences.edit { putLong(KEY_LAST_SYNC, value) }

    /**
     * Debug-only switch written by the instrumentation runner. A seeded QA
     * session must never reach the Spring Boot host, otherwise device tests
     * would depend on a running backend.
     */
    val syncBlocked: Boolean
        get() = BuildConfig.DEBUG && preferences.getBoolean(KEY_QA_OFFLINE, false)

    /** Installs a session without a backend; only valid in debug builds. */
    fun seedDebugSession(token: String, user: AccountUser) {
        check(BuildConfig.DEBUG) { "QA sessions are debug-only" }
        preferences.edit {
            putString(KEY_TOKEN, token)
            putString(KEY_USER, gson.toJson(user))
            putLong(KEY_LOCAL_OWNER, user.id)
            putLong(KEY_SYNC_REVISION, 1L)
            putBoolean(KEY_LOCAL_DIRTY, false)
            putBoolean(KEY_QA_OFFLINE, true)
        }
        _session.value = StoredAccount(token, user)
    }

    fun saveSession(token: String, user: AccountUser) {
        preferences.edit {
            putString(KEY_TOKEN, token)
            putString(KEY_USER, gson.toJson(user))
        }
        _session.value = StoredAccount(token, user)
    }

    fun updateUser(user: AccountUser) {
        val token = token ?: return
        preferences.edit { putString(KEY_USER, gson.toJson(user)) }
        _session.value = StoredAccount(token, user)
    }

    fun clearSession() {
        preferences.edit {
            remove(KEY_TOKEN)
            remove(KEY_USER)
        }
        _session.value = null
    }

    fun resetLocalOwnership() {
        localOwnerUserId = NO_OWNER
        syncRevision = 0L
        localDirty = false
        lastSyncedAt = 0L
    }

    private fun readSession(): StoredAccount? {
        val token = preferences.getString(KEY_TOKEN, null) ?: return null
        val json = preferences.getString(KEY_USER, null) ?: return null
        val user = runCatching { gson.fromJson(json, AccountUser::class.java) }.getOrNull()
            ?: return null
        return StoredAccount(token, user)
    }

    companion object {
        const val NO_OWNER = -1L
        const val PREFERENCES_NAME = "orbit_account"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
        private const val KEY_LOCAL_OWNER = "local_owner"
        private const val KEY_SYNC_REVISION = "sync_revision"
        private const val KEY_LOCAL_DIRTY = "local_dirty"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_QA_OFFLINE = "qa_offline"
    }
}
