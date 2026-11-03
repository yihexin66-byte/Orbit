package com.example.orbit.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiErrorMapperTest {

    @Test
    fun stableAiCodesMapToTheirKinds() {
        val cases = mapOf(
            "AI_NOT_CONFIGURED" to AiFailureKind.NOT_CONFIGURED,
            "AI_QUOTA_EXCEEDED" to AiFailureKind.QUOTA_EXCEEDED,
            "AI_TIMEOUT" to AiFailureKind.TIMEOUT,
            "AI_BUSY" to AiFailureKind.BUSY,
            "AI_INPUT_INVALID" to AiFailureKind.INVALID_INPUT,
            "AI_INVALID_OUTPUT" to AiFailureKind.INVALID_OUTPUT,
            "AI_NO_DATA" to AiFailureKind.NO_DATA,
            "AI_UNAVAILABLE" to AiFailureKind.UNAVAILABLE,
        )

        cases.forEach { (code, kind) ->
            val failure = AiErrorMapper.from(statusCode = 503, code = code, message = null)
            assertEquals(code, kind, failure.kind)
        }
    }

    @Test
    fun authCodesWinOverHttpStatus() {
        assertEquals(
            AiFailureKind.AUTH,
            AiErrorMapper.from(403, "UNAUTHORIZED", "登录已过期").kind,
        )
        assertEquals(
            AiFailureKind.AUTH,
            AiErrorMapper.from(400, "USER_NOT_FOUND", null).kind,
        )
    }

    @Test
    fun unknownCodesFallBackToHttpStatus() {
        assertEquals(AiFailureKind.AUTH, AiErrorMapper.from(401, "SOMETHING", null).kind)
        assertEquals(AiFailureKind.QUOTA_EXCEEDED, AiErrorMapper.from(429, null, null).kind)
        assertEquals(AiFailureKind.TIMEOUT, AiErrorMapper.from(504, "", null).kind)
        assertEquals(AiFailureKind.NETWORK, AiErrorMapper.from(0, null, null).kind)
        assertEquals(AiFailureKind.UNKNOWN, AiErrorMapper.from(500, null, null).kind)
        assertEquals(AiFailureKind.UNKNOWN, AiErrorMapper.from(200, "WHATEVER", null).kind)
    }

    @Test
    fun serverMessageIsTrimmedAndBlankBecomesNull() {
        assertEquals(
            "今日额度已用完",
            AiErrorMapper.from(429, null, "  今日额度已用完  ").serverMessage,
        )
        assertNull(AiErrorMapper.from(429, null, "   ").serverMessage)
        assertNull(AiErrorMapper.from(429, null, null).serverMessage)
    }

    @Test
    fun helpersProduceNetworkAndUnknown() {
        assertEquals(AiFailureKind.NETWORK, AiErrorMapper.network().kind)
        assertEquals(AiFailureKind.UNKNOWN, AiErrorMapper.unknown().kind)
    }
}
