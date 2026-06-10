package com.opencode2phone.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.opencode2phone.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<SessionEntity>)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun count(): Int
}
