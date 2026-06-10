package com.opencode2phone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.opencode2phone.data.local.dao.MessageDao
import com.opencode2phone.data.local.dao.SessionDao
import com.opencode2phone.data.local.entity.MessageEntity
import com.opencode2phone.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
}
