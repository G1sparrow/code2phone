package com.opencode2phone.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StreamEventDto(
    @Json(name = "type") val type: String,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "sessionID") val sessionID: String? = null,
    @Json(name = "messageID") val messageID: String? = null,
    @Json(name = "part") val part: PartDto? = null
)

@JsonClass(generateAdapter = true)
data class PartDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "sessionID") val sessionID: String? = null,
    @Json(name = "messageID") val messageID: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "input") val input: Any? = null,
    @Json(name = "output") val output: Any? = null
)

@JsonClass(generateAdapter = true)
data class WSInitMessage(
    @Json(name = "type") val type: String = "init",
    @Json(name = "sessionId") val sessionId: String? = null
)

@JsonClass(generateAdapter = true)
data class WSUserMessage(
    @Json(name = "type") val type: String = "user",
    @Json(name = "message") val message: String,
    @Json(name = "model") val model: String? = null
)
