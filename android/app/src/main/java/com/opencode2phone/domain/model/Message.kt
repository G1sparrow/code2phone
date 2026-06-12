package com.opencode2phone.domain.model

data class Message(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val reasoning: String? = null,
    val toolCalls: List<ToolCallInfo> = emptyList(),
    val toolResults: List<ToolResultInfo> = emptyList(),
    val createdAt: Long
)

data class ToolCallInfo(
    val name: String,
    val input: String
)

data class ToolResultInfo(
    val name: String,
    val output: String
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
