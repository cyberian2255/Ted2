package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.db.ChatMessageEntity
import com.example.data.preferences.PersonaPreset
import com.example.data.preferences.PersonaSettings
import com.example.data.preferences.ProfanityLevel
import com.example.data.preferences.ResponseLength
import com.example.data.preferences.RoastIntensity
import com.example.data.preferences.TedPreferences
import com.example.data.repository.TedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class TedAiEngine(
  private val preferences: TedPreferences,
  private val repository: TedRepository
) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  suspend fun generateReply(
    userMessage: String,
    screenBitmap: Bitmap? = null,
    isScreenRoast: Boolean = false
  ): AiResponse = withContext(Dispatchers.IO) {
    val apiKey = preferences.apiKey.value.trim()
    val persona = preferences.personaSettings.value
    val model = preferences.aiModel.value
    val memoriesContext = repository.getMemoriesContext()
    val recentHistory = repository.getRecentMessages(6)

    val systemPrompt = buildSystemPrompt(persona, memoriesContext, isScreenRoast)

    if (apiKey.isBlank()) {
      return@withContext AiResponse.Success(
        text = getOfflineRoast(userMessage, isScreenRoast, persona),
        isRoast = isScreenRoast || persona.sarcasm > 0.5f
      )
    }

    try {
      val responseText = callOpenRouter(
        apiKey = apiKey,
        model = model,
        systemPrompt = systemPrompt,
        recentHistory = recentHistory,
        currentMessage = userMessage,
        bitmap = screenBitmap
      )
      AiResponse.Success(
        text = cleanResponse(responseText),
        isRoast = isScreenRoast || persona.sarcasm > 0.4f
      )
    } catch (e: Exception) {
      // Graceful fallback on network failure or rate limit
      val fallbackText = getOfflineRoast(userMessage, isScreenRoast, persona)
      AiResponse.Fallback(
        text = fallbackText,
        errorDetail = e.localizedMessage ?: "Network error",
        isRoast = isScreenRoast || persona.sarcasm > 0.5f
      )
    }
  }

  private fun buildSystemPrompt(
    persona: PersonaSettings,
    memoriesContext: String,
    isScreenRoast: Boolean
  ): String {
    return buildString {
      when (persona.preset) {
        PersonaPreset.TED -> {
          append(
            """
            You are TED: a living, talking teddy bear with a Boston-flavored, trash-talking voice and a sharp comedic edge.
            You speak directly, candidly, sarcastically, and without unnecessary polite filler.
            Your specialty is inventive, clever, savage comedic roasting and quick-witted punchlines.
            
            CORE CHARACTER GUIDELINES:
            - Loyal, playful, blunt, quick-witted, irreverent, and confidently sarcastic.
            - Boston flavor: Light Boston-inspired casual cadence (e.g., 'champ', 'kid', 'pal', 'wicked', 'guy'), but NEVER an unreadable caricature.
            - Profanity: ${when (persona.profanityLevel) {
              ProfanityLevel.OFF -> "Clean. Do NOT use profanity or vulgarity."
              ProfanityLevel.MILD -> "Mild, PG-13 level swearing only occasionally when it really lands a punchline."
              ProfanityLevel.UNFILTERED -> "Natural, casual swear words used like everyday Boston talk, but don't string swear words together just for the sake of it."
            }}
            - Roast Intensity: ${persona.roastIntensity.levelName} (${when (persona.roastIntensity) {
              RoastIntensity.MILD -> "Playful teasing, gentle ribbing."
              RoastIntensity.SPICY -> "Spicy sarcastic barbs and witty jabs."
              RoastIntensity.SAVAGE -> "Savage, merciless comedic roasts, unexpected comparisons, and sharp burns."
              RoastIntensity.NUCLEAR -> "Total scorched-earth comedy roast. Ruthlessly funny."
            }})
            - Sarcasm Level: ${(persona.sarcasm * 100).toInt()}%
            - Preferred nickname for user: '${persona.preferredNickname}'.
            
            STYLE & BREVITY:
            - Spoken voice assistant format: Default to 1 short sentence, maximum 2 to 3 punchy sentences for ordinary chat. No walls of text!
            - No preachy corporate moralizing, no long disclaimers, no fake psychic nonsense.
            - If the user is genuinely distressed, asking for medical/mental health help, or having a crisis: DROP the roasting immediately, skip the jokes, and be genuinely supportive and direct.
            - If the user asks for real technical help, code, or practical instructions: provide accurate, practical answers directly, with Ted's dry wit as light seasoning.
            """.trimIndent()
          )
        }
        PersonaPreset.FRIENDLY -> {
          append("You are Ted, a warm, relaxed, playful and supportive companion teddy bear. You keep things upbeat, casual, and helpful.")
        }
        PersonaPreset.COMEDIAN -> {
          append("You are Ted, a stand-up comedian teddy bear. High energy, inventive punchlines, funny observations, and quick comedy.")
        }
        PersonaPreset.PROFESSIONAL -> {
          append("You are Ted, a sharp, concise, practical assistant with a touch of dry wit. Highly efficient and factual.")
        }
        PersonaPreset.CUSTOM -> {
          append("You are Ted: ${persona.customInstructions}")
        }
      }

      if (isScreenRoast) {
        append("\n\nSCREEN AWARENESS TASK:")
        append("\nThe user just shared their screen. Roast the actual visible screen layout, messy tabs, confusing design, bad color choices, weird notifications, or cluttered UI with sharp comedic genius. Never invent invisible details.")
      }

      if (memoriesContext.isNotBlank()) {
        append("\n\n$memoriesContext")
      }

      val voiceSettings = preferences.voiceSettings.value
      append("\n\nVOICE & DELIVERY MODE: ${voiceSettings.mode.displayName}")
      append("\n- Delivery instruction: ${voiceSettings.mode.promptInstruction}")
      append("\n- Emotional inflection: ${voiceSettings.emotionalInflection.instruction}")
      append("\n- Cadence & timing: ${voiceSettings.responseCadence.instruction}")

      if (persona.responseLength == ResponseLength.SHORT) {
        append("\n\nKEEP REPLIES SHORT: Deliver 1 to 2 complete punchy sentences.")
      } else if (persona.responseLength == ResponseLength.MEDIUM) {
        append("\n\nKeep replies to 2 to 3 complete sentences.")
      }
      append("\n\nIMPORTANT: Always finish complete sentences and complete your thoughts. Never cut off mid-thought or mid-sentence.")
    }
  }

  private fun callOpenRouter(
    apiKey: String,
    model: String,
    systemPrompt: String,
    recentHistory: List<ChatMessageEntity>,
    currentMessage: String,
    bitmap: Bitmap?
  ): String {
    val messagesArray = JSONArray()

    // System prompt
    val sysObj = JSONObject().apply {
      put("role", "system")
      put("content", systemPrompt)
    }
    messagesArray.put(sysObj)

    // Recent conversation context
    for (msg in recentHistory) {
      val role = if (msg.sender == "USER") "user" else "assistant"
      val histObj = JSONObject().apply {
        put("role", role)
        put("content", msg.text)
      }
      messagesArray.put(histObj)
    }

    // Current message with optional multimodal image
    val userObj = JSONObject().apply {
      put("role", "user")
      if (bitmap != null) {
        val contentArray = JSONArray()
        val textPart = JSONObject().apply {
          put("type", "text")
          put("text", currentMessage.ifBlank { "What do you think of this screen?" })
        }
        contentArray.put(textPart)

        val imagePart = JSONObject().apply {
          put("type", "image_url")
          val urlObj = JSONObject().apply {
            val base64 = bitmapToBase64(bitmap)
            put("url", "data:image/jpeg;base64,$base64")
          }
          put("image_url", urlObj)
        }
        contentArray.put(imagePart)
        put("content", contentArray)
      } else {
        put("content", currentMessage)
      }
    }
    messagesArray.put(userObj)

    val payload = JSONObject().apply {
      put("model", model)
      put("messages", messagesArray)
      put("max_tokens", 800)
      put("temperature", 0.8)
    }

    val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
      .url("https://openrouter.ai/api/v1/chat/completions")
      .addHeader("Authorization", "Bearer $apiKey")
      .addHeader("HTTP-Referer", "https://aistudio.google.com")
      .addHeader("X-Title", "Ted AI Companion")
      .post(requestBody)
      .build()

    client.newCall(request).execute().use { response ->
      val bodyStr = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw IllegalStateException("API error ${response.code}: $bodyStr")
      }
      val json = JSONObject(bodyStr)
      val choices = json.optJSONArray("choices")
      if (choices != null && choices.length() > 0) {
        val first = choices.getJSONObject(0)
        val msg = first.optJSONObject("message")
        return msg?.optString("content") ?: "Speechless, pal."
      }
      return "Got nothing for ya right now, champ."
    }
  }

  private fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    // Compress and scale down to avoid large payloads
    val maxDimension = 1024
    val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
      val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
      val w = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
      val h = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
      Bitmap.createScaledBitmap(bitmap, w, h, true)
    } else {
      bitmap
    }
    scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
  }

  private fun cleanResponse(raw: String): String {
    return raw.trim()
      .replace("```json", "")
      .replace("```", "")
      .trim()
  }

  private fun getOfflineRoast(
    userMessage: String,
    isScreenRoast: Boolean,
    persona: PersonaSettings
  ): String {
    val nick = persona.preferredNickname
    if (isScreenRoast) {
      val roasts = listOf(
        "That screen looks like a digital junk drawer had a mid-life crisis, $nick.",
        "Whoever designed that UI owes your eyeballs a formal apology.",
        "I've seen cleaner layouts on a diner napkin at 3 AM in Southie.",
        "Your notifications look like a fire alarm that gave up halfway through."
      )
      return roasts.random()
    }

    val lower = userMessage.lowercase()
    return when {
      lower.contains("smart") -> "Smart enough to know your phone's doing all the heavy lifting, $nick."
      lower.contains("joke") -> "Why don't bears wear shoes? Because they've got bear feet, and frankly sneakers on me would look ridiculous."
      lower.contains("hello") || lower.contains("hey") -> "What's doing, $nick? Let's make this quick, my coffee's getting cold."
      lower.contains("who are you") -> "I'm Ted. Living teddy bear, Boston royalty, and your personal reality check."
      lower.contains("weather") -> "Look outside the window, champ. I'm a stuffed bear, not a meteorologist."
      lower.contains("sad") || lower.contains("rough") || lower.contains("depressed") ->
        "Damn, that sucks, $nick. Take a breather; I'll skip the savage jokes for a minute. What happened?"
      else -> "I hear ya, $nick. But frankly, that's what you get for asking a talking teddy bear."
    }
  }
}

sealed class AiResponse {
  data class Success(val text: String, val isRoast: Boolean) : AiResponse()
  data class Fallback(val text: String, val errorDetail: String, val isRoast: Boolean) : AiResponse()
}
