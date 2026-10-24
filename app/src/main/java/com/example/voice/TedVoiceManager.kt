package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.preferences.TedPreferences
import com.example.data.preferences.VoiceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
  IDLE,
  LISTENING,
  PROCESSING,
  SPEAKING
}

class TedVoiceManager(
  private val context: Context,
  private val preferences: TedPreferences,
  private val onSpeechRecognized: (String) -> Unit
) : TextToSpeech.OnInitListener {

  private val mainHandler = Handler(Looper.getMainLooper())
  private var tts: TextToSpeech? = null
  private var speechRecognizer: SpeechRecognizer? = null
  private var isTtsReady = false
  private var consecutiveErrors = 0
  private var isDestroyed = false
  private var currentFinalUtteranceId: String? = null

  /**
   * Internal state flag indicating if Ted is currently speaking.
   * When true, speech recognizer microphone input loop is disabled and cannot auto-interrupt Ted.
   */
  @Volatile
  var isTedSpeaking: Boolean = false
    private set

  private val _isTedSpeakingFlow = MutableStateFlow(false)
  val isTedSpeakingFlow: StateFlow<Boolean> = _isTedSpeakingFlow.asStateFlow()

  private val _voiceState = MutableStateFlow(VoiceState.IDLE)
  val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

  private val _lastTranscript = MutableStateFlow("")
  val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

  private val _availableVoices = MutableStateFlow<List<String>>(emptyList())
  val availableVoices: StateFlow<List<String>> = _availableVoices.asStateFlow()

  init {
    tts = TextToSpeech(context.applicationContext, this)
    mainHandler.post { initSpeechRecognizer() }
  }

  private fun setSpeaking(speaking: Boolean) {
    isTedSpeaking = speaking
    _isTedSpeakingFlow.value = speaking
    if (speaking) {
      _voiceState.value = VoiceState.SPEAKING
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      isTtsReady = true
      tts?.language = Locale.US
      applyVoiceSettings()

      val voices = tts?.voices?.map { it.name }?.filter { it.contains("en", ignoreCase = true) } ?: emptyList()
      _availableVoices.value = voices

      tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          mainHandler.post {
            setSpeaking(true)
            // Ensure microphone audio input cannot interfere while speaking starts
            try {
              speechRecognizer?.cancel()
            } catch (_: Exception) {}
          }
        }

        override fun onDone(utteranceId: String?) {
          // Full Uninterrupted Playback:
          // Resume microphone listening ONLY after TTS completes its full utterance sequence
          val isFinal = utteranceId != null && (utteranceId == currentFinalUtteranceId || utteranceId.startsWith("ted_final_"))
          if (isFinal) {
            mainHandler.post {
              setSpeaking(false)
              _voiceState.value = VoiceState.IDLE
              currentFinalUtteranceId = null

              // Automated hands-free conversational loop:
              val settings = preferences.voiceSettings.value
              if ((settings.continuousHandsFree || settings.autoListenAfterResponse) && !isDestroyed) {
                mainHandler.postDelayed({
                  if (!isDestroyed && !isTedSpeaking && _voiceState.value == VoiceState.IDLE) {
                    startListening()
                  }
                }, 350)
              }
            }
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          val isFinal = utteranceId != null && (utteranceId == currentFinalUtteranceId || utteranceId.startsWith("ted_final_"))
          if (isFinal) {
            mainHandler.post {
              setSpeaking(false)
              _voiceState.value = VoiceState.IDLE
              currentFinalUtteranceId = null
            }
          }
        }
      })
    }
  }

  fun applyVoiceSettings() {
    if (!isTtsReady) return
    val settings = preferences.voiceSettings.value
    tts?.setSpeechRate(settings.speechRate)
    tts?.setPitch(settings.speechPitch)

    if (settings.selectedVoiceName.isNotBlank()) {
      val foundVoice = tts?.voices?.firstOrNull { it.name == settings.selectedVoiceName }
      if (foundVoice != null) {
        tts?.voice = foundVoice
      }
    }
  }

  /**
   * Complete Sentence Buffering & Uninterrupted Playback:
   * 1. Pauses/stops SpeechRecognizer microphone listening loop immediately.
   * 2. Sets internal state flag isTedSpeaking = true.
   * 3. Buffers and queues all complete sentences sequentially using QUEUE_ADD.
   */
  fun speak(text: String) {
    if (!isTtsReady || isDestroyed || text.isBlank()) return

    // Immediately stop microphone listening to prevent any auto-self-interruption or acoustic feedback
    mainHandler.post {
      try {
        speechRecognizer?.cancel()
      } catch (_: Exception) {}
    }

    applyVoiceSettings()

    val sentences = splitIntoSentences(text)
    if (sentences.isEmpty()) return

    setSpeaking(true)

    val batchId = System.currentTimeMillis()
    val finalUtteranceId = "ted_final_${batchId}"
    currentFinalUtteranceId = finalUtteranceId

    for (i in sentences.indices) {
      val sentence = sentences[i]
      val isFirst = (i == 0)
      val isLast = (i == sentences.size - 1)
      val utteranceId = if (isLast) finalUtteranceId else "ted_chunk_${batchId}_$i"

      val params = Bundle().apply {
        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
      }

      val queueMode = if (isFirst) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      tts?.speak(sentence, queueMode, params, utteranceId)
    }
  }

  /**
   * Splits text on punctuation boundaries (. ! ? \n) to form complete buffered sentence tokens.
   */
  private fun splitIntoSentences(text: String): List<String> {
    if (text.isBlank()) return emptyList()

    val cleanText = text.trim()
      .replace("```json", "")
      .replace("```", "")
      .replace("**", "")
      .replace("*", "")
      .replace("###", "")
      .replace("##", "")
      .replace("#", "")

    val regex = Regex("(?<=[.!?\\n])\\s+")
    val rawList = cleanText.split(regex)
    val result = mutableListOf<String>()

    for (chunk in rawList) {
      val trimmed = chunk.trim()
      if (trimmed.isNotBlank()) {
        result.add(trimmed)
      }
    }

    return if (result.isEmpty()) listOf(cleanText) else result
  }

  /**
   * Tap Face to Shut Up (Manual Interruption):
   * 1. Immediately stops TTS playback.
   * 2. Resets isTedSpeaking = false.
   * 3. Immediately restarts or re-enables SpeechRecognizer / hands-free listening state
   *    so the user can speak right away without needing an extra tap.
   */
  fun shutUpAndListen() {
    mainHandler.post {
      if (isDestroyed) return@post
      currentFinalUtteranceId = null
      try {
        tts?.stop()
      } catch (_: Exception) {}
      setSpeaking(false)
      _voiceState.value = VoiceState.IDLE
      // Immediately start or re-enable SpeechRecognizer listening state
      startListening()
    }
  }

  fun stopSpeaking() {
    mainHandler.post {
      currentFinalUtteranceId = null
      try {
        tts?.stop()
      } catch (_: Exception) {}
      setSpeaking(false)
      if (_voiceState.value == VoiceState.SPEAKING) {
        _voiceState.value = VoiceState.IDLE
      }
    }
  }

  fun interruptSpeech() {
    shutUpAndListen()
  }

  private fun initSpeechRecognizer() {
    if (isDestroyed) return
    try {
      if (speechRecognizer != null) {
        speechRecognizer?.destroy()
        speechRecognizer = null
      }
      if (SpeechRecognizer.isRecognitionAvailable(context)) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
          setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
              if (isTedSpeaking) return
              _voiceState.value = VoiceState.LISTENING
              consecutiveErrors = 0
            }

            override fun onBeginningOfSpeech() {
              // Crucial: Audio feedback or ambient sound MUST NOT auto-interrupt Ted while speaking
              if (isTedSpeaking) return
              _voiceState.value = VoiceState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
              if (!isTedSpeaking) {
                _voiceState.value = VoiceState.PROCESSING
              }
            }

            override fun onError(error: Int) {
              if (isTedSpeaking) return
              _voiceState.value = VoiceState.IDLE
              consecutiveErrors++

              val isHandsFree = preferences.voiceSettings.value.continuousHandsFree
              if (isHandsFree && !isDestroyed && consecutiveErrors < 6) {
                val delayMs = if (consecutiveErrors > 2) 1200L else 400L
                mainHandler.postDelayed({
                  if (!isDestroyed && !isTedSpeaking && _voiceState.value == VoiceState.IDLE) {
                    startListening()
                  }
                }, delayMs)
              }
            }

            override fun onResults(results: Bundle?) {
              if (isTedSpeaking) return
              _voiceState.value = VoiceState.IDLE
              consecutiveErrors = 0
              val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              val recognizedText = matches?.firstOrNull()?.trim().orEmpty()
              if (recognizedText.isNotBlank()) {
                _lastTranscript.value = recognizedText

                val lower = recognizedText.lowercase()
                if (lower == "stop" || lower == "shut up" || lower == "quiet" || lower == "pause") {
                  shutUpAndListen()
                  return
                }

                val wakeWordEnabled = preferences.voiceSettings.value.wakeWordEnabled
                if (wakeWordEnabled) {
                  val cleaned = stripWakeWord(recognizedText)
                  onSpeechRecognized(cleaned)
                } else {
                  onSpeechRecognized(recognizedText)
                }
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {
              if (isTedSpeaking) return
              val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
              if (!partial.isNullOrBlank()) {
                _lastTranscript.value = partial
              }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
          })
        }
      }
    } catch (_: Exception) {}
  }

  private fun stripWakeWord(text: String): String {
    val lower = text.lowercase()
    val wakePhrases = listOf("hey ted", "listen up ted", "listen up", "ted")
    for (wake in wakePhrases) {
      if (lower.startsWith(wake)) {
        return text.substring(wake.length).trimStart(',', ' ', ':').ifBlank { "What's up, Ted?" }
      }
    }
    return text
  }

  fun startListening() {
    mainHandler.post {
      if (isDestroyed) return@post
      try {
        if (isTedSpeaking) {
          currentFinalUtteranceId = null
          tts?.stop()
          setSpeaking(false)
        }
        if (speechRecognizer == null) {
          initSpeechRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
        _voiceState.value = VoiceState.LISTENING
      } catch (_: Exception) {
        _voiceState.value = VoiceState.IDLE
      }
    }
  }

  fun stopListening() {
    mainHandler.post {
      try {
        speechRecognizer?.stopListening()
      } catch (_: Exception) {}
      _voiceState.value = VoiceState.IDLE
    }
  }

  fun testVoicePreview(mode: VoiceMode? = null) {
    val activeMode = mode ?: preferences.voiceSettings.value.mode
    val sampleText = when (activeMode) {
      VoiceMode.BOSTON_HEAVY -> "What's doing, guy? Boston royalty right here. Wicked smart."
      VoiceMode.DEEP_GRITTY -> "Yeah, I'm a talking bear. You got a problem with that, champ?"
      VoiceMode.RAPID_FIRE -> "Alright listen up, clock is ticking, what're we roasting today? Let's go!"
      VoiceMode.CHILL_LOW_TEMPO -> "Take it easy, pal. What's the big rush? I'm relaxing."
      VoiceMode.SARCASTIC_MONOTONE -> "I am expressing overwhelming enthusiasm for your existence."
      VoiceMode.CUSTOM -> "Hey champ, this is your custom calibrated voice profile."
    }
    speak(sampleText)
  }

  fun destroy() {
    isDestroyed = true
    mainHandler.removeCallbacksAndMessages(null)
    try {
      currentFinalUtteranceId = null
      tts?.stop()
      tts?.shutdown()
      speechRecognizer?.destroy()
    } catch (_: Exception) {}
  }
}
