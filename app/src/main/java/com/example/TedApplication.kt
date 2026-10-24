package com.example

import android.app.Application
import com.example.actions.TedActionManager
import com.example.ai.AiResponse
import com.example.ai.TedAiEngine
import com.example.data.db.AppDatabase
import com.example.data.preferences.TedPreferences
import com.example.data.repository.TedRepository
import com.example.screen.TedScreenRoastManager
import com.example.voice.TedVoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TedApplication : Application() {

  companion object {
    lateinit var instance: TedApplication
      private set
  }

  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  lateinit var database: AppDatabase
    private set
  lateinit var repository: TedRepository
    private set
  lateinit var preferences: TedPreferences
    private set
  lateinit var aiEngine: TedAiEngine
    private set
  lateinit var voiceManager: TedVoiceManager
    private set
  lateinit var screenRoastManager: TedScreenRoastManager
    private set
  lateinit var actionManager: TedActionManager
    private set

  override fun onCreate() {
    super.onCreate()
    instance = this

    database = AppDatabase.getDatabase(this)
    repository = TedRepository(database)
    preferences = TedPreferences(this)
    aiEngine = TedAiEngine(preferences, repository)
    actionManager = TedActionManager(this)

    screenRoastManager = TedScreenRoastManager(
      context = this,
      aiEngine = aiEngine,
      repository = repository,
      preferences = preferences,
      scope = applicationScope
    )

    voiceManager = TedVoiceManager(
      context = this,
      preferences = preferences,
      onSpeechRecognized = { text ->
        processVoiceInput(text)
      }
    )
  }

  fun processVoiceInput(userText: String) {
    if (userText.isBlank()) return
    applicationScope.launch {
      repository.insertMessage(
        sender = "USER",
        text = userText
      )

      // Check if user is asking for time, battery, weather, or actions
      val lower = userText.lowercase()
      when {
        lower.contains("time") && !lower.contains("weather") -> {
          val res = actionManager.getTimeAndDate()
          repository.insertMessage(sender = "TED", text = res.speechSummary)
          voiceManager.speak(res.speechSummary)
        }
        lower.contains("battery") -> {
          val res = actionManager.getBatteryStatus()
          repository.insertMessage(sender = "TED", text = res.speechSummary)
          voiceManager.speak(res.speechSummary)
        }
        lower.contains("wifi") || lower.contains("network") || lower.contains("internet") -> {
          val res = actionManager.checkNetworkConnectivity()
          repository.insertMessage(sender = "TED", text = res.speechSummary)
          voiceManager.speak(res.speechSummary)
        }
        lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ") -> {
          val res = actionManager.launchAppByNameOrPackage(userText)
          repository.insertMessage(sender = "TED", text = res.speechSummary)
          voiceManager.speak(res.speechSummary)
        }
        lower.contains("roast my screen") || lower.contains("what's on my screen") || lower.contains("roast this screen") -> {
          screenRoastManager.captureScreen(isRoastMode = true) {}
        }
        else -> {
          // Standard AI generation
          val response = aiEngine.generateReply(
            userMessage = userText,
            isScreenRoast = false
          )
          val replyText = when (response) {
            is AiResponse.Success -> response.text
            is AiResponse.Fallback -> response.text
          }
          repository.insertMessage(
            sender = "TED",
            text = replyText,
            isRoast = when (response) {
              is AiResponse.Success -> response.isRoast
              is AiResponse.Fallback -> response.isRoast
            }
          )
          voiceManager.speak(replyText)
        }
      }
    }
  }

  override fun onTerminate() {
    voiceManager.destroy()
    super.onTerminate()
  }
}
