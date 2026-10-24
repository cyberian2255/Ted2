package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PersonaPreset(val displayName: String, val description: String) {
  TED("Ted (Default)", "Blunt, sarcastic Boston teddy-bear roaster. Savage wit."),
  FRIENDLY("Friendly", "Warm, relaxed, playful and supportive."),
  COMEDIAN("Comedian", "High-energy, focused on clever jokes and punchlines."),
  PROFESSIONAL("Professional", "Direct, concise, practical with mild dry humor."),
  CUSTOM("Custom", "Your own fully customized rules and persona prompt.")
}

enum class RoastIntensity(val levelName: String) {
  MILD("Mild"),
  SPICY("Spicy"),
  SAVAGE("Savage"),
  NUCLEAR("Nuclear")
}

enum class ProfanityLevel(val levelName: String) {
  OFF("Clean (No swears)"),
  MILD("Occasional / PG-13"),
  UNFILTERED("Natural / Unfiltered")
}

enum class ResponseLength(val label: String) {
  SHORT("Short (1-2 sentences)"),
  MEDIUM("Standard (2-3 sentences)"),
  DETAILED("Detailed (Technical)")
}

enum class EmotionalInflection(val label: String, val instruction: String) {
  DEADPAN("Deadpan", "Deliver with zero emotion, complete dry flat detachment."),
  SUBTLE("Subtle", "Subtle dry wit with minimal vocal fluctuation."),
  EXPRESSIVE("Expressive", "Full expressive vocal energy and punch."),
  EXAGGERATED("Exaggerated", "Over-the-top theatrical sarcasm and high energy.")
}

enum class ResponseCadence(val label: String, val instruction: String) {
  SNAPPY("Snappy & Direct", "Keep sentences clipped, direct, and fast."),
  DELIBERATE("Deliberate", "Speak deliberately, weighing each sarcastic word."),
  STACCATO("Staccato (Rapid)", "Rapid-fire punchy phrases with quick pauses."),
  RELAXED("Relaxed Drawl", "Slouching, lazy, unhurried comedic delivery.")
}

enum class VoiceMode(
  val displayName: String,
  val description: String,
  val defaultPitch: Float,
  val defaultRate: Float,
  val defaultInflection: EmotionalInflection,
  val defaultCadence: ResponseCadence,
  val promptInstruction: String
) {
  BOSTON_HEAVY(
    "Boston Heavy",
    "Thick Boston cadence, sharp attitude, unapologetic punchlines.",
    0.92f, 1.12f,
    EmotionalInflection.EXPRESSIVE, ResponseCadence.SNAPPY,
    "Speak with a pronounced Boston cadence. Drop subtle 'r' sounds, use terms like 'wicked', 'guy', 'champ', and punch each sentence directly."
  ),
  DEEP_GRITTY(
    "Deep & Gritty",
    "Low, cynical rumble of a tired veteran teddy bear.",
    0.78f, 0.98f,
    EmotionalInflection.DEADPAN, ResponseCadence.DELIBERATE,
    "Speak in a low, gravelly, cynical tone like a world-weary barfly teddy bear. Deep dry wit, no fluff."
  ),
  RAPID_FIRE(
    "Rapid Fire / High Energy",
    "Fast-talking, chaotic comedy with razor-sharp speed.",
    1.08f, 1.30f,
    EmotionalInflection.EXAGGERATED, ResponseCadence.STACCATO,
    "Speak fast, chaotic, and relentlessly sharp with rapid-fire comedic punchlines and immediate comebacks."
  ),
  CHILL_LOW_TEMPO(
    "Chill / Low Tempo",
    "Lazy deadpan drawl, completely unbothered.",
    0.88f, 0.90f,
    EmotionalInflection.SUBTLE, ResponseCadence.RELAXED,
    "Speak slowly, in an unbothered, slouching, relaxed comedic drawl. Take your time delivering roasts."
  ),
  SARCASTIC_MONOTONE(
    "Sarcastic Monotone",
    "Flat, emotionless, clinical roast delivery.",
    1.00f, 1.05f,
    EmotionalInflection.DEADPAN, ResponseCadence.SNAPPY,
    "Deliver every roast in an absolute flat deadpan monotone. No excitement, just pure clinical savagery."
  ),
  CUSTOM(
    "Custom Profile",
    "Your personalized blend of pitch, rate, inflection, and cadence.",
    1.00f, 1.10f,
    EmotionalInflection.EXPRESSIVE, ResponseCadence.SNAPPY,
    "Deliver witty sarcastic remarks aligned with the user's custom cadence."
  )
}

data class PersonaSettings(
  val preset: PersonaPreset = PersonaPreset.TED,
  val sarcasm: Float = 0.9f,
  val roastIntensity: RoastIntensity = RoastIntensity.SAVAGE,
  val profanityLevel: ProfanityLevel = ProfanityLevel.UNFILTERED,
  val humorFrequency: Float = 0.85f,
  val energy: Float = 0.8f,
  val friendliness: Float = 0.6f,
  val formality: Float = 0.2f,
  val responseLength: ResponseLength = ResponseLength.SHORT,
  val preferredNickname: String = "Champ",
  val preferredJokeStyles: String = "Sarcastic & Savage",
  val seriousInCrisis: Boolean = true,
  val customInstructions: String = ""
)

data class VoiceSettings(
  val mode: VoiceMode = VoiceMode.BOSTON_HEAVY,
  val speechRate: Float = 1.12f,
  val speechPitch: Float = 0.92f,
  val emotionalInflection: EmotionalInflection = EmotionalInflection.EXPRESSIVE,
  val responseCadence: ResponseCadence = ResponseCadence.SNAPPY,
  val selectedVoiceName: String = "",
  val autoListenAfterResponse: Boolean = true,
  val wakeWordEnabled: Boolean = true,
  val interruptibility: Boolean = true,
  val continuousHandsFree: Boolean = true
)

data class ScreenRoastSettings(
  val isRoastMode: Boolean = true
)

class TedPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("ted_preferences", Context.MODE_PRIVATE)

  private val defaultApiKey = "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"

  private val _personaSettings = MutableStateFlow(loadPersonaSettings())
  val personaSettings: StateFlow<PersonaSettings> = _personaSettings.asStateFlow()

  private val _voiceSettings = MutableStateFlow(loadVoiceSettings())
  val voiceSettings: StateFlow<VoiceSettings> = _voiceSettings.asStateFlow()

  private val _screenRoastSettings = MutableStateFlow(loadScreenRoastSettings())
  val screenRoastSettings: StateFlow<ScreenRoastSettings> = _screenRoastSettings.asStateFlow()

  private val _apiKey = MutableStateFlow(loadApiKey())
  val apiKey: StateFlow<String> = _apiKey.asStateFlow()

  private val _aiModel = MutableStateFlow(prefs.getString("ai_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash")
  val aiModel: StateFlow<String> = _aiModel.asStateFlow()

  private fun loadApiKey(): String {
    val saved = prefs.getString("api_key", null)
    if (!saved.isNullOrBlank()) return saved
    return try {
      val buildConfigKey = BuildConfig::class.java.getField("OPENROUTER_API_KEY").get(null) as? String
      if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_OPENROUTER_API_KEY") buildConfigKey else defaultApiKey
    } catch (_: Exception) {
      defaultApiKey
    }
  }

  fun updateApiKey(newKey: String) {
    prefs.edit().putString("api_key", newKey.trim()).apply()
    _apiKey.value = newKey.trim()
  }

  fun updateAiModel(model: String) {
    prefs.edit().putString("ai_model", model).apply()
    _aiModel.value = model
  }

  private fun loadPersonaSettings(): PersonaSettings {
    val presetStr = prefs.getString("persona_preset", PersonaPreset.TED.name) ?: PersonaPreset.TED.name
    val preset = try { PersonaPreset.valueOf(presetStr) } catch (_: Exception) { PersonaPreset.TED }

    val roastStr = prefs.getString("roast_intensity", RoastIntensity.SAVAGE.name) ?: RoastIntensity.SAVAGE.name
    val roast = try { RoastIntensity.valueOf(roastStr) } catch (_: Exception) { RoastIntensity.SAVAGE }

    val profanityStr = prefs.getString("profanity_level", ProfanityLevel.UNFILTERED.name) ?: ProfanityLevel.UNFILTERED.name
    val profanity = try { ProfanityLevel.valueOf(profanityStr) } catch (_: Exception) { ProfanityLevel.UNFILTERED }

    val lengthStr = prefs.getString("response_length", ResponseLength.SHORT.name) ?: ResponseLength.SHORT.name
    val length = try { ResponseLength.valueOf(lengthStr) } catch (_: Exception) { ResponseLength.SHORT }

    return PersonaSettings(
      preset = preset,
      sarcasm = prefs.getFloat("sarcasm", 0.9f),
      roastIntensity = roast,
      profanityLevel = profanity,
      humorFrequency = prefs.getFloat("humor_freq", 0.85f),
      energy = prefs.getFloat("energy", 0.8f),
      friendliness = prefs.getFloat("friendliness", 0.6f),
      formality = prefs.getFloat("formality", 0.2f),
      responseLength = length,
      preferredNickname = prefs.getString("preferred_nickname", "Champ") ?: "Champ",
      preferredJokeStyles = prefs.getString("joke_styles", "Sarcastic & Savage") ?: "Sarcastic & Savage",
      seriousInCrisis = prefs.getBoolean("serious_in_crisis", true),
      customInstructions = prefs.getString("custom_instructions", "") ?: ""
    )
  }

  fun savePersonaSettings(settings: PersonaSettings) {
    prefs.edit()
      .putString("persona_preset", settings.preset.name)
      .putFloat("sarcasm", settings.sarcasm)
      .putString("roast_intensity", settings.roastIntensity.name)
      .putString("profanity_level", settings.profanityLevel.name)
      .putFloat("humor_freq", settings.humorFrequency)
      .putFloat("energy", settings.energy)
      .putFloat("friendliness", settings.friendliness)
      .putFloat("formality", settings.formality)
      .putString("response_length", settings.responseLength.name)
      .putString("preferred_nickname", settings.preferredNickname)
      .putString("joke_styles", settings.preferredJokeStyles)
      .putBoolean("serious_in_crisis", settings.seriousInCrisis)
      .putString("custom_instructions", settings.customInstructions)
      .apply()
    _personaSettings.value = settings
  }

  private fun loadVoiceSettings(): VoiceSettings {
    val modeStr = prefs.getString("voice_mode", VoiceMode.BOSTON_HEAVY.name) ?: VoiceMode.BOSTON_HEAVY.name
    val mode = try { VoiceMode.valueOf(modeStr) } catch (_: Exception) { VoiceMode.BOSTON_HEAVY }

    val inflectionStr = prefs.getString("emotional_inflection", mode.defaultInflection.name) ?: mode.defaultInflection.name
    val inflection = try { EmotionalInflection.valueOf(inflectionStr) } catch (_: Exception) { mode.defaultInflection }

    val cadenceStr = prefs.getString("response_cadence", mode.defaultCadence.name) ?: mode.defaultCadence.name
    val cadence = try { ResponseCadence.valueOf(cadenceStr) } catch (_: Exception) { mode.defaultCadence }

    return VoiceSettings(
      mode = mode,
      speechRate = prefs.getFloat("speech_rate", mode.defaultRate),
      speechPitch = prefs.getFloat("speech_pitch", mode.defaultPitch),
      emotionalInflection = inflection,
      responseCadence = cadence,
      selectedVoiceName = prefs.getString("selected_voice", "") ?: "",
      autoListenAfterResponse = prefs.getBoolean("auto_listen", true),
      wakeWordEnabled = prefs.getBoolean("wake_word", true),
      interruptibility = prefs.getBoolean("interruptibility", true),
      continuousHandsFree = prefs.getBoolean("continuous_hands_free", true)
    )
  }

  fun saveVoiceSettings(settings: VoiceSettings) {
    prefs.edit()
      .putString("voice_mode", settings.mode.name)
      .putFloat("speech_rate", settings.speechRate)
      .putFloat("speech_pitch", settings.speechPitch)
      .putString("emotional_inflection", settings.emotionalInflection.name)
      .putString("response_cadence", settings.responseCadence.name)
      .putString("selected_voice", settings.selectedVoiceName)
      .putBoolean("auto_listen", settings.autoListenAfterResponse)
      .putBoolean("wake_word", settings.wakeWordEnabled)
      .putBoolean("interruptibility", settings.interruptibility)
      .putBoolean("continuous_hands_free", settings.continuousHandsFree)
      .apply()
    _voiceSettings.value = settings
  }

  private fun loadScreenRoastSettings(): ScreenRoastSettings {
    return ScreenRoastSettings(
      isRoastMode = prefs.getBoolean("screen_roast_mode", true)
    )
  }

  fun saveScreenRoastSettings(settings: ScreenRoastSettings) {
    prefs.edit()
      .putBoolean("screen_roast_mode", settings.isRoastMode)
      .apply()
    _screenRoastSettings.value = settings
  }
}
