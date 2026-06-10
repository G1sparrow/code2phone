package com.opencode2phone.domain.model

data class Message(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val reasoning: String? = null,
    val createdAt: Long
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
