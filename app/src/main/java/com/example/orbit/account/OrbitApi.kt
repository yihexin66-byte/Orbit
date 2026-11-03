package com.example.orbit.account

import com.example.orbit.BuildConfig
import com.example.orbit.ai.AiChatRequest
import com.example.orbit.ai.AiChatResponse
import com.example.orbit.ai.AiPlanRequest
import com.example.orbit.ai.AiPlanResponse
import com.example.orbit.ai.AiReviewRequest
import com.example.orbit.ai.AiReviewResponse
import com.example.orbit.ai.AiStatusResponse
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

interface OrbitApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>

    @GET("api/me")
    suspend fun profile(@Header("Authorization") authorization: String): AccountUser

    @PATCH("api/me")
    suspend fun updateNickname(
        @Header("Authorization") authorization: String,
        @Body body: NicknameRequest,
    ): AccountUser

    @POST("api/me/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") authorization: String,
        @Body body: AvatarUploadRequest,
    ): AccountUser

    @GET("api/sync")
    suspend fun pullSync(@Header("Authorization") authorization: String): SyncSnapshot

    @PUT("api/sync")
    suspend fun pushSync(
        @Header("Authorization") authorization: String,
        @Body body: SyncPushRequest,
    ): Response<SyncSnapshot>
}

/**
 * AI endpoints live on their own Retrofit instance because a planning call can
 * legitimately take longer than the interactive account/sync timeout.
 */
interface OrbitAiService {

    @GET("api/ai/status")
    suspend fun aiStatus(@Header("Authorization") authorization: String): AiStatusResponse

    @POST("api/ai/plan")
    suspend fun aiPlan(
        @Header("Authorization") authorization: String,
        @Body body: AiPlanRequest,
    ): AiPlanResponse

    @POST("api/ai/review")
    suspend fun aiReview(
        @Header("Authorization") authorization: String,
        @Body body: AiReviewRequest,
    ): AiReviewResponse

    @POST("api/ai/chat")
    suspend fun aiChat(
        @Header("Authorization") authorization: String,
        @Body body: AiChatRequest,
    ): AiChatResponse
}

class OrbitApiException(
    val statusCode: Int,
    val code: String?,
    override val message: String,
) : Exception(message)

object OrbitApi {

    val baseUrl: String = BuildConfig.ORBIT_API_BASE_URL

    private fun newClient(readTimeoutSeconds: Long): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()

    private val client = newClient(20)

    private val aiClient = newClient(35)

    val service: OrbitApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OrbitApiService::class.java)

    val aiService: OrbitAiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(aiClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OrbitAiService::class.java)

    fun avatarUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("http")) path else baseUrl.trimEnd('/') + path
    }
}
