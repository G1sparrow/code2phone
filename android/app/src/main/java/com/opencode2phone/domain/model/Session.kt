package com.opencode2phone.domain.model

data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val projectId: String,
    val directory: String,
    val messageCount: Int = 0
)
