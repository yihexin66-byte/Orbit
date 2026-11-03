package com.example.orbit.account

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException
import java.io.File

class AccountRepository(
    private val api: OrbitApiService = OrbitApi.service,
) {

    private val gson = Gson()

    suspend fun register(username: String, password: String): StoredAccount =
        call { api.register(AuthRequest(username.trim(), password)) }.toStoredAccount()

    suspend fun login(username: String, password: String): StoredAccount =
        call { api.login(AuthRequest(username.trim(), password)) }.toStoredAccount()

    suspend fun logout(token: String) {
        runCatching { api.logout(bearer(token)) }
    }

    suspend fun profile(token: String): AccountUser = call { api.profile(bearer(token)) }

    suspend fun updateNickname(token: String, nickname: String): AccountUser =
        call { api.updateNickname(bearer(token), NicknameRequest(nickname.trim())) }

    suspend fun uploadAvatar(token: String, file: File): AccountUser {
        if (file.length() > MAX_AVATAR_BYTES) {
            throw OrbitApiException(413, "AVATAR_TOO_LARGE", "头像不能超过 1.4 MB，请重新选择照片")
        }
        val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        return call { api.uploadAvatar(bearer(token), AvatarUploadRequest(encoded)) }
    }

    suspend fun pull(token: String): SyncSnapshot = call { api.pullSync(bearer(token)) }

    suspend fun push(token: String, snapshot: SyncPushRequest): SyncPushResult {
        try {
            val response = api.pushSync(bearer(token), snapshot)
            if (response.isSuccessful) {
                val body = response.body()
                    ?: throw OrbitApiException(500, "EMPTY_BODY", "服务器没有返回同步结果")
                return SyncPushResult(accepted = true, snapshot = body)
            }
            if (response.code() == 409) {
                val error = response.errorBody()?.string()
                val conflict = error?.let {
                    runCatching { gson.fromJson(it, SyncSnapshot::class.java) }.getOrNull()
                } ?: throw OrbitApiException(409, "SYNC_CONFLICT", "云端数据已更新")
                return SyncPushResult(accepted = false, snapshot = conflict)
            }
            throw parseHttp(response.code(), response.errorBody()?.string())
        } catch (exception: IOException) {
            throw OrbitApiException(0, "NETWORK", "无法连接服务器，请确认后端已启动")
        }
    }

    private suspend fun <T> call(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: HttpException) {
            throw parseHttp(exception.code(), exception.response()?.errorBody()?.string())
        } catch (exception: IOException) {
            Log.w(LOG_TAG, "push failed", exception)
            throw OrbitApiException(0, "NETWORK", "无法连接服务器，请确认后端已启动")
        }
    }

    private fun parseHttp(status: Int, body: String?): OrbitApiException {
        val parsed = body?.let {
            runCatching { gson.fromJson(it, ApiErrorBody::class.java) }.getOrNull()
        }
        val message = when {
            !parsed?.message.isNullOrBlank() -> parsed?.message.orEmpty()
            status == 401 -> "用户名或密码不正确"
            else -> "请求失败（$status）"
        }
        return OrbitApiException(status, parsed?.code, message)
    }

    private fun AuthResponse.toStoredAccount(): StoredAccount = StoredAccount(token, user)

    private fun bearer(token: String): String = "Bearer $token"

    private companion object {
        const val LOG_TAG = "OrbitAccount"
        const val MAX_AVATAR_BYTES = 1_400L * 1024L
    }
}
