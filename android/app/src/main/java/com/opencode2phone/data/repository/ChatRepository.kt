package com.opencode2phone.data.repository

import com.opencode2phone.data.local.dao.MessageDao
import com.opencode2phone.data.local.entity.MessageEntity
import com.opencode2phone.data.remote.OpencodeWebSocket
import com.opencode2phone.data.remote.dto.StreamEventDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val webSocket: OpencodeWebSocket,
    private val messageDao: MessageDao
) {
    fun connect(
        host: String,
        port: Int,
        sessionId: String? = null,
        firstMessage: String? = null,
        model: String? = null
    ): Flow<StreamEventDto> {
        return webSocket.connect(host, port, sessionId, firstMessage, model)
    }

    fun disconnect() {
        webSocket.disconnect()
    }

    suspend fun cacheMessage(message: MessageEntity) {
        messageDao.upsert(message)
    }
}
