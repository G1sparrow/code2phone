package com.opencode2phone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.opencode2phone.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeBySession(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getBySession(sessionId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}
