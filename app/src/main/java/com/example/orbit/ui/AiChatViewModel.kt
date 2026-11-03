package com.example.orbit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbit.account.OrbitAccountGraph
import com.example.orbit.ai.AiChatContext
import com.example.orbit.ai.AiChatMessage
import com.example.orbit.ai.AiChatRequest
import com.example.orbit.ai.AiChatRole
import com.example.orbit.ai.AiChatTurn
import com.example.orbit.ai.AiFailure
import com.example.orbit.ai.AiFailureKind
import com.example.orbit.ai.AiRepository
import com.example.orbit.ai.AiServiceException
import com.example.orbit.ai.AiStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val status: AiStatus? = null,
    val statusLoading: Boolean = false,
    val statusError: AiFailure? = null,
    val failure: AiFailure? = null,
    val qaBlocked: Boolean = false,
) {
    val configured: Boolean get() = status?.configured == true
    val chatRemaining: Int get() = status?.chatRemaining ?: 0
    val canSend: Boolean get() = input.isNotBlank() && !sending
    val hasConversation: Boolean get() = messages.isNotEmpty()
}

/**
 * Keeps the assistant conversation in memory only. The cloud gateway is
 * stateless and the transcript is capped; nothing is written to disk, so a
 * closed app leaves no chat history behind.
 */
class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val store = OrbitAccountGraph.store
    private val repository = AiRepository()
    private var nextMessageId = 0L

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun refreshStatus() {
        if (store.syncBlocked) {
            _uiState.update {
                it.copy(qaBlocked = true, statusLoading = false, statusError = null)
            }
            return
        }
        val token = store.token
        if (token == null) {
            _uiState.update {
                it.copy(
                    statusLoading = false,
                    statusError = AiFailure(AiFailureKind.AUTH),
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(statusLoading = state.status == null, statusError = null)
        }
        viewModelScope.launch {
            try {
                val status = repository.status(token)
                _uiState.update {
                    it.copy(
                        status = status,
                        statusLoading = false,
                        statusError = null,
                        qaBlocked = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: AiServiceException) {
                _uiState.update {
                    it.copy(statusLoading = false, statusError = exception.failure)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(statusLoading = false, statusError = AiFailure(AiFailureKind.UNKNOWN))
                }
            }
        }
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value.take(MAX_MESSAGE_LENGTH), failure = null) }
    }

    fun send(context: AiChatContext) {
        sendText(_uiState.value.input, context)
    }

    fun sendText(text: String, context: AiChatContext) {
        val state = _uiState.value
        if (state.sending) return
        val messageText = text.trim().take(MAX_MESSAGE_LENGTH)
        if (messageText.isEmpty()) return
        val token = prepareSend() ?: return
        val history = historyOf(state.messages)
        val userMessage = AiChatMessage(
            id = nextMessageId++,
            role = AiChatRole.USER,
            text = messageText,
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                sending = true,
                failure = null,
            )
        }
        transmit(token, messageText, history, context)
    }

    fun retry(context: AiChatContext) {
        val state = _uiState.value
        if (state.sending) return
        val lastUserIndex = state.messages.indexOfLast { it.role == AiChatRole.USER }
        if (lastUserIndex < 0) return
        val lastUser = state.messages[lastUserIndex]
        val token = prepareSend() ?: return
        val history = historyOf(state.messages.take(lastUserIndex))
        _uiState.update { it.copy(sending = true, failure = null) }
        transmit(token, lastUser.text, history, context)
    }

    fun dismissFailure() {
        _uiState.update { it.copy(failure = null) }
    }

    fun clearConversation() {
        if (_uiState.value.sending) return
        _uiState.update {
            it.copy(messages = emptyList(), failure = null, input = "")
        }
    }

    private fun prepareSend(): String? {
        if (store.syncBlocked) {
            _uiState.update {
                it.copy(failure = AiFailure(AiFailureKind.NOT_CONFIGURED))
            }
            return null
        }
        val token = store.token
        if (token == null) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.AUTH)) }
            return null
        }
        val status = _uiState.value.status
        if (status != null && !status.configured) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.NOT_CONFIGURED)) }
            return null
        }
        if (status != null && status.chatRemaining <= 0) {
            _uiState.update { it.copy(failure = AiFailure(AiFailureKind.QUOTA_EXCEEDED)) }
            return null
        }
        return token
    }

    private fun transmit(
        token: String,
        text: String,
        history: List<AiChatTurn>,
        context: AiChatContext,
    ) {
        viewModelScope.launch {
            try {
                val response = repository.sendChat(
                    token = token,
                    request = AiChatRequest(
                        message = text,
                        history = history,
                        context = context,
                    ),
                )
                val message = AiChatMessage(
                    id = nextMessageId++,
                    role = AiChatRole.ASSISTANT,
                    text = response.reply.trim(),
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + message,
                        sending = false,
                        failure = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: AiServiceException) {
                _uiState.update {
                    it.copy(sending = false, failure = exception.failure)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(sending = false, failure = AiFailure(AiFailureKind.UNKNOWN))
                }
            } finally {
                refreshQuietly()
            }
        }
    }

    private fun historyOf(messages: List<AiChatMessage>): List<AiChatTurn> =
        messages
            .filter { it.text.isNotBlank() }
            .takeLast(MAX_HISTORY_TURNS)
            .map { message ->
                AiChatTurn(
                    role = if (message.role == AiChatRole.ASSISTANT) "assistant" else "user",
                    content = message.text,
                )
            }

    private fun refreshQuietly() {
        if (store.syncBlocked) return
        val token = store.token ?: return
        viewModelScope.launch {
            try {
                val status = repository.status(token)
                _uiState.update { it.copy(status = status, statusError = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The turn already surfaced its own failure; quota refresh is best effort.
            }
        }
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 600
        const val MAX_HISTORY_TURNS = 8
    }
}
