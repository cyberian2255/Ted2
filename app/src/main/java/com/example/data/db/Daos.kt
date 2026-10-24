package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
  @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
  fun getAllMessages(): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
  suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessageEntity): Long

  @Query("DELETE FROM chat_messages WHERE id = :id")
  suspend fun deleteMessage(id: Long)

  @Query("DELETE FROM chat_messages")
  suspend fun clearAll()
}

@Dao
interface MemoryDao {
  @Query("SELECT * FROM ted_memories ORDER BY timestamp DESC")
  fun getAllMemories(): Flow<List<MemoryEntity>>

  @Query("SELECT * FROM ted_memories")
  suspend fun getMemoriesList(): List<MemoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemory(memory: MemoryEntity): Long

  @Query("DELETE FROM ted_memories WHERE id = :id")
  suspend fun deleteMemory(id: Long)

  @Query("DELETE FROM ted_memories")
  suspend fun clearAll()
}
