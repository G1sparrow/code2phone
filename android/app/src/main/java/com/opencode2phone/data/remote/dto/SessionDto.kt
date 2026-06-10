package com.opencode2phone.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionListResponse(
    @Json(name = "sessions") val sessions: List<SessionDto>
)

@JsonClass(generateAdapter = true)
data class SessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String?,
    @Json(name = "created") val created: Long,
    @Json(name = "updated") val updated: Long,
    @Json(name = "projectId") val projectId: String?,
    @Json(name = "directory") val directory: String?,
    @Json(name = "messageCount") val messageCount: Int? = 0
)

@JsonClass(generateAdapter = true)
data class SessionDetailResponse(
    @Json(name = "session") val session: SessionDto,
    @Json(name = "messages") val messages: List<MessageDto>?
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String,
    @Json(name = "createdAt") val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class DirectoryListResponse(
    @Json(name = "directories") val directories: List<String>
)
