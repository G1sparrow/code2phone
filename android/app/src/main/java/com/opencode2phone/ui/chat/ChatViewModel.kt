package com.opencode2phone.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode2phone.data.local.entity.MessageEntity
import com.opencode2phone.data.repository.ChatRepository
import com.opencode2phone.data.repository.ModelRepository
import com.opencode2phone.data.repository.SessionRepository
import com.opencode2phone.di.ServerConfig
import com.opencode2phone.domain.model.Message
import com.opencode2phone.domain.model.MessageRole
import com.opencode2phone.domain.model.OpencodeModel
import com.opencode2phone.domain.model.ToolCallInfo
import com.opencode2phone.domain.model.ToolResultInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isStreaming: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val inputText: String = "",
    val error: String? = null,
    val sessionId: String? = null,
    val streamingContent: String = "",
    val streamingReasoning: String = "",
    val streamingToolCalls: List<ToolCallInfo> = emptyList(),
    val streamingToolResults: List<ToolResultInfo> = emptyList(),
    val models: List<OpencodeModel> = emptyList(),
    val selectedModel: OpencodeModel? = null,
    val showModelPicker: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionRepository: SessionRepository,
    private val modelRepository: ModelRepository,
    private val serverConfig: ServerConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    fun initSession(sessionId: String?) {
        if (sessionId.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(sessionId = null)
            loadModels()
            return
        }
        _uiState.value = _uiState.value.copy(sessionId = sessionId, isLoadingMessages = true)
        loadSessionMessages(sessionId)
        loadModels()
    }

    private fun loadSessionMessages(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.observeMessages(sessionId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
        viewModelScope.launch {
            sessionRepository.loadSessionDetail(sessionId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoadingMessages = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMessages = false,
                        error = e.message
                    )
                }
            )
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            val result = modelRepository.getModels()
            result.onSuccess { models ->
                _uiState.value = _uiState.value.copy(
                    models = models,
                    selectedModel = models.firstOrNull()
                )
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun toggleModelPicker() {
        _uiState.value = _uiState.value.copy(showModelPicker = !_uiState.value.showModelPicker)
    }

    fun selectModel(model: OpencodeModel) {
        _uiState.value = _uiState.value.copy(selectedModel = model, showModelPicker = false)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isStreaming) return

        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            sessionId = _uiState.value.sessionId ?: "",
            role = MessageRole.USER,
            content = text,
            createdAt = System.currentTimeMillis()
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            inputText = "",
            isStreaming = true,
            error = null,
            streamingContent = "",
            streamingReasoning = "",
            streamingToolCalls = emptyList(),
            streamingToolResults = emptyList()
        )

        viewModelScope.launch {
            chatRepository.cacheMessage(
                MessageEntity(
                    id = userMessage.id,
                    sessionId = userMessage.sessionId,
                    role = "user",
                    content = userMessage.content,
                    createdAt = userMessage.createdAt
                )
            )
        }

        startStreaming(text)
    }

    private fun startStreaming(message: String) {
        streamingJob?.cancel()
        chatRepository.disconnect()
        streamingJob = viewModelScope.launch {
            try {
                chatRepository.connect(
                    host = serverConfig.host,
                    port = serverConfig.port,
                    sessionId = _uiState.value.sessionId,
                    firstMessage = message,
                    model = _uiState.value.selectedModel?.let { "${it.providerID}/${it.id}" }
                ).collect { event ->
                    handleStreamEvent(event)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isStreaming = false,
                    error = e.message ?: "Stream error"
                )
            }
        }
    }

    private fun handleStreamEvent(event: com.opencode2phone.data.remote.dto.StreamEventDto) {
        when (event.type) {
            "init" -> {
                event.sessionID?.let { sessionId ->
                    _uiState.value = _uiState.value.copy(sessionId = sessionId)
                }
            }
            "text" -> {
                val text = event.part?.text ?: ""
                _uiState.value = _uiState.value.copy(
                    streamingContent = _uiState.value.streamingContent + text
                )
            }
            "reasoning" -> {
                val text = event.part?.text ?: ""
                _uiState.value = _uiState.value.copy(
                    streamingReasoning = _uiState.value.streamingReasoning + text
                )
            }
            "tool_call" -> {
                val name = event.part?.name ?: "tool"
                val input = event.part?.input?.let { serializeAny(it) } ?: "{}"
                val call = ToolCallInfo(name = name, input = input)
                _uiState.value = _uiState.value.copy(
                    streamingToolCalls = _uiState.value.streamingToolCalls + call
                )
            }
            "tool_result" -> {
                val name = event.part?.name ?: "tool"
                val output = event.part?.output?.let { serializeAny(it) } ?: ""
                val result = ToolResultInfo(name = name, output = output)
                _uiState.value = _uiState.value.copy(
                    streamingToolResults = _uiState.value.streamingToolResults + result
                )
            }
            "step_finish" -> {
                val fullContent = _uiState.value.streamingContent
                val reasoningContent = _uiState.value.streamingReasoning
                val toolCalls = _uiState.value.streamingToolCalls
                val toolResults = _uiState.value.streamingToolResults

                val assistantMessage = Message(
                    id = event.messageID ?: UUID.randomUUID().toString(),
                    sessionId = _uiState.value.sessionId ?: "",
                    role = MessageRole.ASSISTANT,
                    content = fullContent,
                    reasoning = reasoningContent.ifBlank { null },
                    toolCalls = toolCalls,
                    toolResults = toolResults,
                    createdAt = System.currentTimeMillis()
                )

                val hasContent = fullContent.isNotBlank() || reasoningContent.isNotBlank() || toolCalls.isNotEmpty() || toolResults.isNotEmpty()

                if (hasContent) {
                    viewModelScope.launch {
                        chatRepository.cacheMessage(
                            MessageEntity(
                                id = assistantMessage.id,
                                sessionId = assistantMessage.sessionId,
                                role = "assistant",
                                content = assistantMessage.content,
                                reasoning = assistantMessage.reasoning,
                                toolCallsJson = SessionRepository.toolCallsInfoToJson(toolCalls),
                                toolResultsJson = SessionRepository.toolResultsInfoToJson(toolResults),
                                createdAt = assistantMessage.createdAt
                            )
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        isStreaming = false,
                        streamingContent = "",
                        streamingReasoning = "",
                        streamingToolCalls = emptyList(),
                        streamingToolResults = emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isStreaming = false,
                        streamingContent = "",
                        streamingReasoning = "",
                        streamingToolCalls = emptyList(),
                        streamingToolResults = emptyList()
                    )
                }
            }
            "error" -> {
                val errorMsg = event.part?.error ?: "Unknown error"
                _uiState.value = _uiState.value.copy(
                    isStreaming = false,
                    error = errorMsg
                )
            }
            "step_start" -> {
                // No display content
            }
        }
    }

    private fun serializeAny(value: Any): String {
        return when (value) {
            is String -> value
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> org.json.JSONObject(value as Map<String, Any>).toString(2)
            is List<*> -> org.json.JSONArray(value as List<Any>).toString(2)
            else -> value.toString()
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        chatRepository.disconnect()
        _uiState.value = _uiState.value.copy(isStreaming = false)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        streamingJob?.cancel()
        chatRepository.disconnect()
        super.onCleared()
    }
}
