package com.example.orbit.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.orbit.R
import com.example.orbit.ai.AiFailure
import com.example.orbit.ai.AiFailureKind

/**
 * One place for the Chinese fallback copy. The gateway already sends a
 * user-facing sentence for most failures, so its message wins when present.
 */
@Composable
internal fun aiFailureText(failure: AiFailure): String {
    val fallback = when (failure.kind) {
        AiFailureKind.NOT_CONFIGURED -> stringResource(R.string.ai_plan_error_not_configured)
        AiFailureKind.QUOTA_EXCEEDED -> stringResource(R.string.ai_plan_error_quota)
        AiFailureKind.TIMEOUT -> stringResource(R.string.ai_plan_error_timeout)
        AiFailureKind.BUSY -> stringResource(R.string.ai_plan_error_busy)
        AiFailureKind.INVALID_INPUT -> stringResource(R.string.ai_plan_error_invalid_input)
        AiFailureKind.INVALID_OUTPUT -> stringResource(R.string.ai_plan_error_invalid_output)
        AiFailureKind.NO_DATA -> stringResource(R.string.ai_plan_error_no_data)
        AiFailureKind.AUTH -> stringResource(R.string.ai_plan_error_auth)
        AiFailureKind.NETWORK -> stringResource(R.string.ai_plan_error_network)
        AiFailureKind.UNAVAILABLE -> stringResource(R.string.ai_plan_error_unavailable)
        AiFailureKind.UNKNOWN -> stringResource(R.string.ai_plan_error_unknown)
    }
    return failure.serverMessage?.takeIf { it.isNotBlank() } ?: fallback
}
