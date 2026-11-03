package com.example.orbit.ui

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.account.AccountUser
import com.example.orbit.account.AvatarProcessor
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.account.OrbitApiException
import com.example.orbit.account.StoredAccount
import com.example.orbit.account.SyncPhase
import com.example.orbit.account.SyncStatus
import com.example.orbit.ai.AiReviewCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AuthMode {
    LOGIN,
    REGISTER,
}

data class AccountUiState(
    val session: StoredAccount? = null,
    val sync: SyncStatus = SyncStatus(),
    val mode: AuthMode = AuthMode.LOGIN,
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nicknameDraft: String = "",
    val editingNickname: Boolean = false,
    val busy: Boolean = false,
    val avatarBusy: Boolean = false,
    /** Login succeeded and the first MySQL pull/replace is still running. */
    val initialSyncing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val user: AccountUser? get() = session?.user
}

/**
 * Owns the account session, the editable profile and avatar uploads. Cloud sync
 * itself lives in [com.example.orbit.account.OrbitSyncManager]; this class only
 * mirrors its status so the profile screen can render it.
 */
class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val graph = OrbitAccountGraph
    private val store = graph.store
    private val repository = graph.repository
    private val syncManager = graph.syncManager
    private val avatarProcessor = AvatarProcessor(application)
    private val aiReviewCache = AiReviewCache(application)
    private val app: Application = application

    private val _uiState = MutableStateFlow(
        AccountUiState(
            session = store.session.value,
            sync = syncManager.status.value,
            nicknameDraft = store.session.value?.user?.nickname.orEmpty(),
        ),
    )
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private var pendingCameraFile: File? = null
    private var pendingCameraUri: Uri? = null

    init {
        viewModelScope.launch {
            store.session.collect { session ->
                _uiState.update { state ->
                    state.copy(
                        session = session,
                        nicknameDraft = if (state.editingNickname) {
                            state.nicknameDraft
                        } else {
                            session?.user?.nickname.orEmpty()
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            syncManager.status.collect { status ->
                _uiState.update { state -> state.copy(sync = status) }
            }
        }
    }

    fun setMode(mode: AuthMode) {
        _uiState.update {
            it.copy(mode = mode, error = null, password = "", confirmPassword = "")
        }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value.take(USERNAME_MAX), error = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value.take(PASSWORD_MAX), error = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value.take(PASSWORD_MAX), error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.busy) return
        val username = state.username.trim()
        val validation = when {
            username.isEmpty() -> "请输入用户名"
            !USERNAME_PATTERN.matches(username) -> "用户名需为 3–24 位字母、数字或下划线"
            state.password.isEmpty() -> "请输入密码"
            state.password.length < PASSWORD_MIN -> "密码至少需要 6 位"
            state.mode == AuthMode.REGISTER && state.password != state.confirmPassword ->
                "两次输入的密码不一致"

            else -> null
        }
        if (validation != null) {
            _uiState.update { it.copy(error = validation) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null, message = null) }
            try {
                val account = if (state.mode == AuthMode.REGISTER) {
                    repository.register(username, state.password)
                } else {
                    repository.login(username, state.password)
                }
                // A cache owned by another user (or by pre-account installs)
                // must not be shown or uploaded under this account.
                syncManager.prepareForAccount(account.user.id)
                store.saveSession(account.token, account.user)
                _uiState.update {
                    it.copy(
                        busy = false,
                        initialSyncing = true,
                        password = "",
                        confirmPassword = "",
                        username = "",
                        error = null,
                        message = null,
                    )
                }
                syncManager.onSignedIn()
                try {
                    // Block the main UI until the MySQL snapshot (or its empty
                    // state) has replaced the local cache. A failed pull still
                    // opens the app, but only with this account's own rows.
                    syncManager.syncNow()
                } finally {
                    _uiState.update { it.copy(initialSyncing = false) }
                }
            } catch (failure: OrbitApiException) {
                _uiState.update {
                    it.copy(busy = false, initialSyncing = false, error = failure.message)
                }
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        busy = false,
                        initialSyncing = false,
                        error = failure.message ?: "请求失败",
                    )
                }
            }
        }
    }

    fun logout() {
        val token = _uiState.value.session?.token ?: return
        if (_uiState.value.busy) return
        val userId = store.userId
        // The gate flips on the next frame. Cache cleanup and the server-side
        // token revocation continue in the background so leaving never waits
        // for a network round trip.
        store.clearSession()
        syncManager.onSignedOut()
        _uiState.update {
            AccountUiState(sync = SyncStatus())
        }
        viewModelScope.launch {
            syncManager.clearLocalCache()
            userId?.let(aiReviewCache::clearUser)
            runCatching { repository.logout(token) }
        }
    }

    fun startNicknameEdit() {
        val user = _uiState.value.user ?: return
        _uiState.update {
            it.copy(editingNickname = true, nicknameDraft = user.nickname, error = null)
        }
    }

    fun updateNicknameDraft(value: String) {
        _uiState.update {
            it.copy(nicknameDraft = value.take(NICKNAME_MAX), error = null)
        }
    }

    fun cancelNicknameEdit() {
        val user = _uiState.value.user
        _uiState.update {
            it.copy(
                editingNickname = false,
                nicknameDraft = user?.nickname.orEmpty(),
                error = null,
            )
        }
    }

    fun saveNickname() {
        val state = _uiState.value
        val token = state.session?.token ?: return
        if (state.busy) return
        val nickname = state.nicknameDraft.trim()
        when {
            nickname.isEmpty() -> {
                _uiState.update { it.copy(error = "昵称不能为空") }
                return
            }

            nickname.length > NICKNAME_MAX -> {
                _uiState.update { it.copy(error = "昵称最多 24 个字符") }
                return
            }

            nickname == state.user?.nickname -> {
                _uiState.update { it.copy(editingNickname = false, error = null) }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                val user = repository.updateNickname(token, nickname)
                store.updateUser(user)
                _uiState.update {
                    it.copy(
                        busy = false,
                        editingNickname = false,
                        nicknameDraft = user.nickname,
                        message = "昵称已更新",
                    )
                }
            } catch (failure: OrbitApiException) {
                if (handleExpiredSession(failure)) return@launch
                _uiState.update { it.copy(busy = false, error = failure.message) }
            } catch (failure: Exception) {
                _uiState.update { it.copy(busy = false, error = failure.message ?: "保存失败") }
            }
        }
    }

    /** Creates the FileProvider target for ACTION_IMAGE_CAPTURE. */
    fun createCameraTarget(): Uri? {
        if (store.session.value == null) return null
        return try {
            val directory = File(app.cacheDir, "camera").apply { mkdirs() }
            val file = File(directory, "avatar-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                file,
            )
            pendingCameraFile?.delete()
            pendingCameraFile = file
            pendingCameraUri = uri
            uri
        } catch (failure: Exception) {
            _uiState.update { it.copy(error = failure.message ?: "无法打开相机") }
            null
        }
    }

    fun onCameraResult(success: Boolean) {
        val file = pendingCameraFile
        val uri = pendingCameraUri
        pendingCameraFile = null
        pendingCameraUri = null
        if (!success) {
            file?.delete()
            return
        }
        if (file == null || uri == null) {
            _uiState.update { it.copy(error = "没有拍到满意的照片，请重试") }
            return
        }
        uploadAvatar(uri, file)
    }

    fun onGalleryPicked(uri: Uri?) {
        if (uri == null) return
        uploadAvatar(uri, null)
    }

    private fun uploadAvatar(uri: Uri, sourceFile: File?) {
        val token = store.token
        if (token == null) {
            sourceFile?.delete()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(avatarBusy = true, error = null, message = null) }
            var processed: File? = null
            try {
                processed = withContext(Dispatchers.IO) { avatarProcessor.prepare(uri) }
                val user = repository.uploadAvatar(token, processed)
                store.updateUser(user)
                _uiState.update {
                    it.copy(avatarBusy = false, message = "头像已更新")
                }
            } catch (failure: OrbitApiException) {
                if (handleExpiredSession(failure)) return@launch
                _uiState.update { it.copy(avatarBusy = false, error = failure.message) }
            } catch (failure: Exception) {
                _uiState.update {
                    it.copy(
                        avatarBusy = false,
                        error = failure.message ?: "图片处理失败，请换一张试试",
                    )
                }
            } finally {
                withContext(Dispatchers.IO) {
                    processed?.delete()
                    sourceFile?.delete()
                }
            }
        }
    }

    fun requestSync() {
        syncManager.requestSync()
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Profile calls bypass the sync manager, so an expired token has to be
     * handled here too: drop the session and the account-owned cache, then let
     * the login gate take over.
     */
    private suspend fun handleExpiredSession(failure: OrbitApiException): Boolean {
        if (failure.statusCode != 401) return false
        val userId = store.userId
        store.clearSession()
        syncManager.onSignedOut()
        syncManager.clearLocalCache()
        userId?.let(aiReviewCache::clearUser)
        _uiState.update { AccountUiState(error = "登录已过期，请重新登录") }
        return true
    }

    companion object {
        private const val USERNAME_MAX = 24
        private const val PASSWORD_MAX = 64
        private const val PASSWORD_MIN = 6
        private const val NICKNAME_MAX = 24
        private val USERNAME_PATTERN = Regex("[A-Za-z0-9_]{3,24}")
    }
}
