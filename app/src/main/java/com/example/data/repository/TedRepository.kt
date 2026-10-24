package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.MemoryEntity
import kotlinx.coroutines.flow.Flow

class TedRepository(private val db: AppDatabase) {
  val allMessages: Flow<List<ChatMessageEntity>> = db.chatMessageDao().getAllMessages()
  val allMemories: Flow<List<MemoryEntity>> = db.memoryDao().getAllMemories()

  suspend fun insertMessage(
    sender: String,
    text: String,
    isRoast: Boolean = false,
    imageUri: String? = null
  ): Long {
    val message = ChatMessageEntity(
      sender = sender,
      text = text,
      isRoast = isRoast,
      imageUri = imageUri
    )
    return db.chatMessageDao().insertMessage(message)
  }

  suspend fun deleteMessage(id: Long) {
    db.chatMessageDao().deleteMessage(id)
  }

  suspend fun clearChat() {
    db.chatMessageDao().clearAll()
  }

  suspend fun getRecentMessages(limit: Int = 10): List<ChatMessageEntity> {
    return db.chatMessageDao().getRecentMessages(limit).reversed()
  }

  suspend fun addMemory(key: String, value: String, category: String = "general"): Long {
    val memory = MemoryEntity(
      key = key.trim(),
      value = value.trim(),
      category = category
    )
    return db.memoryDao().insertMemory(memory)
  }

  suspend fun deleteMemory(id: Long) {
    db.memoryDao().deleteMemory(id)
  }

  suspend fun clearMemories() {
    db.memoryDao().clearAll()
  }

  suspend fun getMemoriesContext(): String {
    val list = db.memoryDao().getMemoriesList()
    if (list.isEmpty()) return ""
    return buildString {
      append("KNOWN USER FACTS & PERSONALIZATION:\n")
      for (mem in list) {
        append("- [${mem.category.uppercase()}] ${mem.key}: ${mem.value}\n")
      }
    }
  }
}
