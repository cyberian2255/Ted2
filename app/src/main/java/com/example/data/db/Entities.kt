package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sender: String, // "USER" or "TED"
  val text: String,
  val isRoast: Boolean = false,
  val imageUri: String? = null,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ted_memories")
data class MemoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val key: String,
  val value: String,
  val category: String = "general", // "nickname", "likes", "dislikes", "habits", "general"
  val timestamp: Long = System.currentTimeMillis()
)
