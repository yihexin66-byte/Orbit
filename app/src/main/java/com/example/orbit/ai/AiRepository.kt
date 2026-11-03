package com.example.orbit.ai

import com.example.orbit.account.ApiErrorBody
import com.example.orbit.account.OrbitAiService
import com.example.orbit.account.OrbitApi
import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException

/**
 * Thin client for the AI gateway. It only classifies failures; generated plans
 * and reviews are normalized on the server, so the app never talks to Coze and
 * never sees the provider token.
 */
class AiRepository(
    private val api: OrbitAiService = OrbitApi.aiService,
) {

    private val gson = Gson()

    suspend fun status(token: String): AiStatus =
        call { api.aiStatus(bearer(token)) }.toStatus()

    suspend fun generatePlan(token: String, request: AiPlanRequest): AiPlanResponse {
        val response = call { api.aiPlan(bearer(token), request) }
        val plan = response.plan
        if (plan == null || plan.stages.isEmpty() || plan.taskCount == 0) {
            throw AiServiceException(AiFailure(AiFailureKind.INVALID_OUTPUT))
        }
        return response
    }

    suspend fun generateReview(token: String, request: AiReviewRequest): AiReviewResponse {
        val response = call { api.aiReview(bearer(token), request) }
        val review = response.review
        if (review == null || review.headline.isBlank()) {
            throw AiServiceException(AiFailure(AiFailureKind.INVALID_OUTPUT))
        }
        return response
    }

    suspend fun sendChat(token: String, request: AiChatRequest): AiChatResponse {
        val response = call { api.aiChat(bearer(token), request) }
        if (response.reply.isBlank()) {
            throw AiServiceException(AiFailure(AiFailureKind.INVALID_OUTPUT))
        }
        return response
    }

    private suspend fun <T> call(block: suspend () -> T): T {
        try {
            return block()
        } catch (exception: HttpException) {
            throw failure(
                status = exception.code(),
                body = exception.response()?.errorBody()?.string(),
            )
        } catch (exception: IOException) {
            throw AiServiceException(AiErrorMapper.network())
        }
    }

    private fun failure(status: Int, body: String?): AiServiceException {
        val parsed = body?.let {
            runCatching { gson.fromJson(it, ApiErrorBody::class.java) }.getOrNull()
        }
        return AiServiceException(
            AiErrorMapper.from(
                statusCode = status,
                code = parsed?.code,
                message = parsed?.message,
            ),
        )
    }

    private fun bearer(token: String): String = "Bearer $token"
}
