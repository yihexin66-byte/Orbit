package com.example.orbit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbit.R
import com.example.orbit.ai.AiChatMessage
import com.example.orbit.ai.AiChatRole
import com.example.orbit.ai.AiFailure
import com.example.orbit.ai.AiFailureKind
import com.example.orbit.ui.AiChatUiState
import com.example.orbit.ui.components.AiSparkGlyph
import com.example.orbit.ui.components.OrbitCard
import com.example.orbit.ui.components.OrbitIconButton
import com.example.orbit.ui.theme.OrbitCorners
import com.example.orbit.ui.theme.orbitColors

const val AiAssistantSheetTag = "ai_assistant_sheet"
const val AiAssistantInputTag = "ai_assistant_input"
const val AiAssistantSendTag = "ai_assistant_send"
const val AiAssistantWelcomeTag = "ai_assistant_welcome"
const val AiAssistantQuotaTag = "ai_assistant_quota"

/**
 * Bottom-sheet conversation surface. It owns only presentation state; the
 * transcript, sending flag and quota live in [AiChatViewModel] so dismissing
 * the sheet never loses the current conversation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    state: AiChatUiState,
    onDismiss: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onRefreshStatus: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenLearning: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onClearConversation: () -> Unit,
    onDismissFailure: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { onRefreshStatus() }
    // The typing bubble and the arriving reply share one index, so the reply
    // length has to be part of the key or a long answer stays clipped under
    // the composer. Scroll to the slot right after the last message: it is the
    // typing bubble while sending and the quick actions once the reply lands.
    val lastReplyLength = state.messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(state.messages.size, state.sending, state.failure, lastReplyLength) {
        if (state.messages.isEmpty()) return@LaunchedEffect
        runCatching { listState.animateScrollToItem(state.messages.size) }
    }

    val chatAvailable = !state.qaBlocked &&
        (state.status == null || (state.configured && state.chatRemaining > 0))
    val inputEnabled = !state.sending && chatAvailable

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        scrimColor = colors.scrim.copy(alpha = 0.78f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 10.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(OrbitCorners.pill))
                    .background(colors.textMuted.copy(alpha = 0.42f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag(AiAssistantSheetTag)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            AssistantHeader(
                state = state,
                onDismiss = onDismiss,
                onClear = onClearConversation,
            )
            Spacer(Modifier.height(10.dp))
            StatusLine(
                state = state,
                onRetryStatus = onRefreshStatus,
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item(key = "welcome") {
                        WelcomePanel(
                            onSendPrompt = onSendPrompt,
                            onOpenPlan = onOpenPlan,
                            onOpenReview = onOpenReview,
                            onOpenLearning = onOpenLearning,
                        )
                    }
                } else {
                    items(state.messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }
                    if (state.sending) {
                        item(key = "typing") { TypingBubble() }
                    }
                    if (state.failure != null) {
                        item(key = "failure") {
                            ChatFailureCard(
                                failure = state.failure,
                                onRetry = {
                                    onDismissFailure()
                                    onRetry()
                                },
                            )
                        }
                    }
                    item(key = "composer_actions") {
                        ComposerActions(
                            onOpenPlan = onOpenPlan,
                            onOpenReview = onOpenReview,
                            onOpenLearning = onOpenLearning,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            AssistantInput(
                state = state,
                enabled = inputEnabled,
                onInputChange = onInputChange,
                onSend = onSend,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ai_assistant_privacy),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AssistantHeader(
    state: AiChatUiState,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiAvatar(size = 38.dp)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.ai_assistant_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(OrbitCorners.pill))
                        .background(colors.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.sp,
                        ),
                        color = if (colors.isDark) colors.primary else colors.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ai_assistant_subtitle),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = colors.textMuted,
            )
        }
        if (state.hasConversation) {
            Text(
                text = stringResource(R.string.ai_assistant_clear),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        OrbitIconButton(
            onClick = onDismiss,
            contentDescription = stringResource(R.string.ai_plan_back),
            icon = Icons.Filled.Close,
            size = 38.dp,
        )
    }
}

@Composable
private fun StatusLine(
    state: AiChatUiState,
    onRetryStatus: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val status = state.status
    val text: String?
    val tint: Color
    when {
        state.qaBlocked -> {
            text = stringResource(R.string.ai_assistant_offline)
            tint = colors.warning
        }

        state.statusLoading -> {
            text = stringResource(R.string.ai_assistant_loading_status)
            tint = colors.textMuted
        }

        status != null && !status.configured -> {
            text = stringResource(R.string.ai_assistant_not_configured)
            tint = colors.warning
        }

        state.statusError != null -> {
            text = stringResource(R.string.ai_assistant_error_default)
            tint = colors.danger
        }

        status != null && status.chatRemaining <= 0 -> {
            text = stringResource(R.string.ai_assistant_quota_exhausted)
            tint = colors.warning
        }

        status != null -> {
            text = stringResource(R.string.ai_assistant_quota, status.chatRemaining)
            tint = colors.textMuted
        }

        else -> {
            text = null
            tint = colors.textMuted
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(colors.track)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(AiAssistantQuotaTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (status?.configured == true && !state.qaBlocked) colors.primary else tint),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text ?: stringResource(R.string.ai_assistant_loading_status),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 0.2.sp,
            ),
            color = tint,
            modifier = Modifier.weight(1f),
        )
        if (state.statusError != null && !state.qaBlocked) {
            Text(
                text = stringResource(R.string.ai_plan_retry),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRetryStatus)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun WelcomePanel(
    onSendPrompt: (String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenLearning: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AiAssistantWelcomeTag),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OrbitCard(padding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AiAvatar(size = 28.dp)
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ai_assistant_welcome),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        ),
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = stringResource(R.string.ai_assistant_welcome_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                        ),
                        color = colors.textMuted,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionTile(
                title = stringResource(R.string.ai_assistant_action_plan),
                description = stringResource(R.string.ai_assistant_action_plan_desc),
                onClick = onOpenPlan,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                title = stringResource(R.string.ai_assistant_action_review),
                description = stringResource(R.string.ai_assistant_action_review_desc),
                onClick = onOpenReview,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                title = stringResource(R.string.ai_assistant_action_learning),
                description = stringResource(R.string.ai_assistant_action_learning_desc),
                onClick = onOpenLearning,
                modifier = Modifier.weight(1f),
            )
        }

        OrbitCard(padding = PaddingValues(16.dp)) {
            Text(
                text = stringResource(R.string.ai_assistant_capability_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.6.sp,
                ),
                color = colors.textMuted,
            )
            Spacer(Modifier.height(10.dp))
            CapabilityRow("01", stringResource(R.string.ai_assistant_capability_1))
            CapabilityRow("02", stringResource(R.string.ai_assistant_capability_2))
            CapabilityRow("03", stringResource(R.string.ai_assistant_capability_3))
            CapabilityRow("04", stringResource(R.string.ai_assistant_capability_4))
            CapabilityRow("05", stringResource(R.string.ai_assistant_capability_5))
        }

        val methodPrompt = stringResource(R.string.ai_assistant_suggestion_method)
        val reviewPrompt = stringResource(R.string.ai_assistant_suggestion_review)
        val poemPrompt = stringResource(R.string.ai_assistant_suggestion_poem)
        val englishPrompt = stringResource(R.string.ai_assistant_suggestion_english)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SuggestionPill(
                    text = methodPrompt,
                    onClick = { onSendPrompt(methodPrompt) },
                )
            }
            item {
                SuggestionPill(
                    text = reviewPrompt,
                    onClick = { onSendPrompt(reviewPrompt) },
                )
            }
            item {
                SuggestionPill(
                    text = poemPrompt,
                    onClick = { onSendPrompt(poemPrompt) },
                )
            }
            item {
                SuggestionPill(
                    text = englishPrompt,
                    onClick = { onSendPrompt(englishPrompt) },
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Column(
        modifier = modifier
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.cardElevated)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(OrbitCorners.inner))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 12.dp),
    ) {
        AiSparkGlyph(
            tint = if (colors.isDark) colors.primary else colors.textPrimary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 11.sp,
                lineHeight = 14.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                lineHeight = 11.5.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textMuted,
        )
    }
}

@Composable
private fun CapabilityRow(index: String, text: String) {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = index,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                letterSpacing = 0.6.sp,
            ),
            color = colors.accentLine,
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun SuggestionPill(
    text: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.5.sp,
            letterSpacing = 0.sp,
        ),
        color = colors.textSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(colors.track)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun ComposerActions(
    onOpenPlan: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenLearning: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        ActionChip(
            text = stringResource(R.string.ai_assistant_action_plan),
            onClick = onOpenPlan,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            text = stringResource(R.string.ai_assistant_action_review),
            onClick = onOpenReview,
            modifier = Modifier.weight(1f),
        )
        ActionChip(
            text = stringResource(R.string.ai_assistant_action_learning),
            onClick = onOpenLearning,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitCorners.pill))
            .background(colors.track)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 0.sp,
            ),
            color = colors.textSecondary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val colors = MaterialTheme.orbitColors
    val isUser = message.role == AiChatRole.USER
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMax = maxWidth * 0.86f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (!isUser) {
                AiAvatar(size = 26.dp)
                Spacer(Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .widthIn(max = bubbleMax)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 6.dp,
                            bottomEnd = if (isUser) 6.dp else 18.dp,
                        ),
                    )
                    .background(if (isUser) colors.textPrimary else colors.track)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    ),
                    color = if (isUser) colors.backgroundTop else colors.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val colors = MaterialTheme.orbitColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiAvatar(size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(colors.track)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.6.dp,
                color = if (colors.isDark) colors.primary else colors.textPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_assistant_sending),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun ChatFailureCard(
    failure: AiFailure,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitCorners.inner))
            .background(colors.danger.copy(alpha = if (colors.isDark) 0.10f else 0.06f))
            .border(
                1.dp,
                colors.danger.copy(alpha = 0.45f),
                RoundedCornerShape(OrbitCorners.inner),
            )
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_assistant_error_title),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = colors.danger,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = chatFailureMessage(failure),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.ai_assistant_retry),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.track)
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun AssistantInput(
    state: AiChatUiState,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val colors = MaterialTheme.orbitColors
    val canSend = enabled && state.input.isNotBlank() && !state.sending
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .testTag(AiAssistantInputTag),
            enabled = enabled,
            placeholder = {
                Text(
                    text = stringResource(R.string.ai_assistant_input_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = colors.textMuted,
                )
            },
            maxLines = 4,
            shape = RoundedCornerShape(22.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.cardBorder,
                disabledBorderColor = colors.cardBorder.copy(alpha = 0.6f),
                focusedContainerColor = colors.track,
                unfocusedContainerColor = colors.track,
                disabledContainerColor = colors.track.copy(alpha = 0.55f),
                cursorColor = colors.primary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                disabledTextColor = colors.textMuted,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(if (canSend) colors.primary else colors.track)
                .clickable(enabled = canSend, onClick = onSend)
                .testTag(AiAssistantSendTag),
            contentAlignment = Alignment.Center,
        ) {
            if (state.sending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.onAccent,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.ai_assistant_send),
                    tint = if (canSend) colors.onAccent else colors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AiAvatar(size: androidx.compose.ui.unit.Dp) {
    val colors = MaterialTheme.orbitColors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        AiSparkGlyph(
            tint = if (colors.isDark) colors.primary else colors.textPrimary,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

@Composable
private fun chatFailureMessage(failure: AiFailure): String = stringResource(
    when (failure.kind) {
        AiFailureKind.NOT_CONFIGURED -> R.string.ai_plan_error_not_configured
        AiFailureKind.QUOTA_EXCEEDED -> R.string.ai_plan_error_quota
        AiFailureKind.TIMEOUT -> R.string.ai_plan_error_timeout
        AiFailureKind.BUSY -> R.string.ai_plan_error_busy
        AiFailureKind.INVALID_INPUT -> R.string.ai_plan_error_invalid_input
        AiFailureKind.INVALID_OUTPUT -> R.string.ai_plan_error_invalid_output
        AiFailureKind.NO_DATA -> R.string.ai_plan_error_no_data
        AiFailureKind.AUTH -> R.string.ai_plan_error_auth
        AiFailureKind.NETWORK -> R.string.ai_plan_error_network
        AiFailureKind.UNAVAILABLE -> R.string.ai_plan_error_unavailable
        AiFailureKind.UNKNOWN -> R.string.ai_assistant_error_default
    },
)
