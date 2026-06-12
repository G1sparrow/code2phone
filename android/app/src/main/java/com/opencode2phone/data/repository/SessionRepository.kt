package com.opencode2phone.data.repository

import com.opencode2phone.data.local.dao.MessageDao
import com.opencode2phone.data.local.dao.SessionDao
import com.opencode2phone.data.local.entity.MessageEntity
import com.opencode2phone.data.local.entity.SessionEntity
import com.opencode2phone.data.remote.OpencodeApi
import com.opencode2phone.domain.model.Message
import com.opencode2phone.domain.model.MessageRole
import com.opencode2phone.domain.model.Session
import com.opencode2phone.domain.model.ToolCallInfo
import com.opencode2phone.domain.model.ToolResultInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val api: OpencodeApi
) {
    fun observeSessions(): Flow<List<Session>> {
        return sessionDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeMessages(sessionId: String): Flow<List<Message>> {
        return messageDao.observeBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun listDirectories(): Result<List<String>> {
        return try {
            val response = api.listDirectories()
            Result.success(response.directories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSessions(directory: String? = null): Result<List<Session>> {
        return try {
            val response = api.listSessions(directory = directory)
            val entities = response.sessions.map { dto ->
                SessionEntity(
                    id = dto.id,
                    title = dto.title ?: "Untitled",
                    createdAt = dto.created,
                    updatedAt = dto.updated,
                    projectId = dto.projectId ?: "",
                    directory = dto.directory ?: "",
                    messageCount = dto.messageCount ?: 0
                )
            }
            sessionDao.upsertAll(entities)
            Result.success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSessionDetail(sessionId: String): Result<Session> {
        return try {
            val response = api.getSession(sessionId)
            val sessionEntity = SessionEntity(
                id = response.session.id,
                title = response.session.title ?: "Untitled",
                createdAt = response.session.created,
                updatedAt = response.session.updated,
                projectId = response.session.projectId ?: "",
                directory = response.session.directory ?: "",
                messageCount = response.messages?.size ?: 0
            )
            sessionDao.upsert(sessionEntity)

            response.messages?.forEach { msgDto ->
                val msgEntity = MessageEntity(
                    id = msgDto.id,
                    sessionId = sessionId,
                    role = msgDto.role,
                    content = msgDto.content,
                    reasoning = msgDto.reasoning,
                    toolCallsJson = toolCallsListToJson(msgDto.toolCalls),
                    toolResultsJson = toolResultsListToJson(msgDto.toolResults),
                    createdAt = msgDto.createdAt
                )
                messageDao.upsert(msgEntity)
            }

            Result.success(sessionEntity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SessionEntity.toDomain() = Session(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        projectId = projectId,
        directory = directory,
        messageCount = messageCount
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        sessionId = sessionId,
        role = when (role) {
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            else -> MessageRole.SYSTEM
        },
        content = content,
        reasoning = reasoning,
        toolCalls = parseToolCallsJson(toolCallsJson),
        toolResults = parseToolResultsJson(toolResultsJson),
        createdAt = createdAt
    )

    companion object {
        fun toolCallsListToJson(toolCalls: List<com.opencode2phone.data.remote.dto.ToolCallDto>?): String? {
            if (toolCalls.isNullOrEmpty()) return null
            return JSONArray(toolCalls.map {
                JSONObject().apply {
                    put("name", it.name)
                    put("input", it.input)
                }
            }).toString()
        }

        fun toolResultsListToJson(toolResults: List<com.opencode2phone.data.remote.dto.ToolResultDto>?): String? {
            if (toolResults.isNullOrEmpty()) return null
            return JSONArray(toolResults.map {
                JSONObject().apply {
                    put("name", it.name)
                    put("output", it.output)
                }
            }).toString()
        }

        fun toolCallsInfoToJson(toolCalls: List<ToolCallInfo>?): String? {
            if (toolCalls.isNullOrEmpty()) return null
            return JSONArray(toolCalls.map {
                JSONObject().apply {
                    put("name", it.name)
                    put("input", it.input)
                }
            }).toString()
        }

        fun toolResultsInfoToJson(toolResults: List<ToolResultInfo>?): String? {
            if (toolResults.isNullOrEmpty()) return null
            return JSONArray(toolResults.map {
                JSONObject().apply {
                    put("name", it.name)
                    put("output", it.output)
                }
            }).toString()
        }

        private fun parseToolCallsJson(json: String?): List<ToolCallInfo> {
            if (json.isNullOrEmpty()) return emptyList()
            return try {
                val array = JSONArray(json)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    ToolCallInfo(
                        name = obj.optString("name", ""),
                        input = obj.optString("input", "{}")
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun parseToolResultsJson(json: String?): List<ToolResultInfo> {
            if (json.isNullOrEmpty()) return emptyList()
            return try {
                val array = JSONArray(json)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    ToolResultInfo(
                        name = obj.optString("name", ""),
                        output = obj.optString("output", "")
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
