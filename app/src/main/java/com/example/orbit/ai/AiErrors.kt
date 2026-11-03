package com.example.orbit.ai

enum class AiFailureKind {
    NOT_CONFIGURED,
    QUOTA_EXCEEDED,
    TIMEOUT,
    BUSY,
    INVALID_INPUT,
    INVALID_OUTPUT,
    NO_DATA,
    AUTH,
    NETWORK,
    UNAVAILABLE,
    UNKNOWN,
}

data class AiFailure(
    val kind: AiFailureKind,
    val serverMessage: String? = null,
)

/** The failure is already classified; the UI owns the localized copy. */
class AiServiceException(val failure: AiFailure) : Exception(failure.serverMessage)

/**
 * Maps the gateway's stable error codes. Keeping this pure makes the mapping
 * testable without Retrofit and keeps Chinese copy out of the data layer.
 */
object AiErrorMapper {

    fun from(statusCode: Int, code: String?, message: String?): AiFailure {
        val cleanMessage = message?.trim()?.takeIf { it.isNotEmpty() }
        val kind = when (code) {
            "AI_NOT_CONFIGURED" -> AiFailureKind.NOT_CONFIGURED
            "AI_QUOTA_EXCEEDED" -> AiFailureKind.QUOTA_EXCEEDED
            "AI_TIMEOUT" -> AiFailureKind.TIMEOUT
            "AI_BUSY" -> AiFailureKind.BUSY
            "AI_INPUT_INVALID" -> AiFailureKind.INVALID_INPUT
            "AI_INVALID_OUTPUT" -> AiFailureKind.INVALID_OUTPUT
            "AI_NO_DATA" -> AiFailureKind.NO_DATA
            "AI_UNAVAILABLE" -> AiFailureKind.UNAVAILABLE
            "UNAUTHORIZED", "USER_NOT_FOUND" -> AiFailureKind.AUTH
            "NETWORK" -> AiFailureKind.NETWORK
            else -> when (statusCode) {
                401 -> AiFailureKind.AUTH
                429 -> AiFailureKind.QUOTA_EXCEEDED
                504 -> AiFailureKind.TIMEOUT
                0 -> AiFailureKind.NETWORK
                else -> AiFailureKind.UNKNOWN
            }
        }
        return AiFailure(kind = kind, serverMessage = cleanMessage)
    }

    fun network() = AiFailure(AiFailureKind.NETWORK)

    fun unknown() = AiFailure(AiFailureKind.UNKNOWN)
}
