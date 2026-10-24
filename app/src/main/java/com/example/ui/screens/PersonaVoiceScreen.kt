package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TedApplication
import com.example.data.preferences.EmotionalInflection
import com.example.data.preferences.PersonaPreset
import com.example.data.preferences.PersonaSettings
import com.example.data.preferences.ProfanityLevel
import com.example.data.preferences.ResponseCadence
import com.example.data.preferences.ResponseLength
import com.example.data.preferences.RoastIntensity
import com.example.data.preferences.VoiceMode
import com.example.data.preferences.VoiceSettings

@Composable
fun PersonaVoiceScreen(modifier: Modifier = Modifier) {
  val app = TedApplication.instance
  val prefs = app.preferences
  val persona by prefs.personaSettings.collectAsState()
  val voice by prefs.voiceSettings.collectAsState()
  val availableVoices by app.voiceManager.availableVoices.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header Card
    ElevatedCard(
      shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      modifier = Modifier.fillMaxWidth().testTag("persona_voice_header")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Personality & Voice Modes",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Switch between Boston Heavy, Deep & Gritty, Rapid Fire, and customize voice cadences.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }
    }

    // Tabs for Persona vs Voice
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Personality & Humor", fontWeight = FontWeight.SemiBold) },
        icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Voice Modes & Audio", fontWeight = FontWeight.SemiBold) },
        icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
    }

    if (selectedTab == 0) {
      PersonaTabContent(
        persona = persona,
        onUpdatePersona = { prefs.savePersonaSettings(it) }
      )
    } else {
      VoiceTabContent(
        voice = voice,
        availableVoices = availableVoices,
        onUpdateVoice = {
          prefs.saveVoiceSettings(it)
          app.voiceManager.applyVoiceSettings()
        },
        onPreviewVoice = { mode -> app.voiceManager.testVoicePreview(mode) }
      )
    }
  }
}

@Composable
fun PersonaTabContent(
  persona: PersonaSettings,
  onUpdatePersona: (PersonaSettings) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Presets
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth().testTag("persona_presets_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Character Preset", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          PersonaPreset.values().forEach { preset ->
            FilterChip(
              selected = persona.preset == preset,
              onClick = {
                val updated = when (preset) {
                  PersonaPreset.TED -> persona.copy(
                    preset = preset,
                    sarcasm = 0.9f,
                    roastIntensity = RoastIntensity.SAVAGE,
                    profanityLevel = ProfanityLevel.UNFILTERED,
                    humorFrequency = 0.85f,
                    energy = 0.8f,
                    friendliness = 0.6f,
                    formality = 0.2f
                  )
                  PersonaPreset.FRIENDLY -> persona.copy(
                    preset = preset,
                    sarcasm = 0.2f,
                    roastIntensity = RoastIntensity.MILD,
                    profanityLevel = ProfanityLevel.OFF,
                    humorFrequency = 0.5f,
                    energy = 0.7f,
                    friendliness = 0.95f,
                    formality = 0.3f
                  )
                  PersonaPreset.COMEDIAN -> persona.copy(
                    preset = preset,
                    sarcasm = 0.7f,
                    roastIntensity = RoastIntensity.SPICY,
                    profanityLevel = ProfanityLevel.MILD,
                    humorFrequency = 1.0f,
                    energy = 0.95f,
                    friendliness = 0.8f,
                    formality = 0.1f
                  )
                  PersonaPreset.PROFESSIONAL -> persona.copy(
                    preset = preset,
                    sarcasm = 0.15f,
                    roastIntensity = RoastIntensity.MILD,
                    profanityLevel = ProfanityLevel.OFF,
                    humorFrequency = 0.2f,
                    energy = 0.6f,
                    friendliness = 0.7f,
                    formality = 0.85f
                  )
                  PersonaPreset.CUSTOM -> persona.copy(preset = preset)
                }
                onUpdatePersona(updated)
              },
              label = { Text(preset.name, fontSize = 11.sp) }
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = persona.preset.description,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Roast Intensity
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth().testTag("roast_intensity_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Roast Intensity", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          RoastIntensity.values().forEach { intensity ->
            FilterChip(
              selected = persona.roastIntensity == intensity,
              onClick = { onUpdatePersona(persona.copy(roastIntensity = intensity)) },
              label = { Text(intensity.levelName, fontSize = 12.sp) }
            )
          }
        }
      }
    }

    // Profanity Level
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Profanity Filter", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ProfanityLevel.values().forEach { level ->
            FilterChip(
              selected = persona.profanityLevel == level,
              onClick = { onUpdatePersona(persona.copy(profanityLevel = level)) },
              label = { Text(level.levelName, fontSize = 11.sp) }
            )
          }
        }
      }
    }

    // Sarcasm & Humor Sliders
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Attitude & Wit Sliders", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Sarcasm: ${(persona.sarcasm * 100).toInt()}%", fontSize = 13.sp)
        Slider(
          value = persona.sarcasm,
          onValueChange = { onUpdatePersona(persona.copy(sarcasm = it)) },
          colors = SliderDefaults.colors(thumbColor = Color(0xFFE89A3C), activeTrackColor = Color(0xFFE89A3C))
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text("Humor Frequency: ${(persona.humorFrequency * 100).toInt()}%", fontSize = 13.sp)
        Slider(
          value = persona.humorFrequency,
          onValueChange = { onUpdatePersona(persona.copy(humorFrequency = it)) },
          colors = SliderDefaults.colors(thumbColor = Color(0xFFE89A3C), activeTrackColor = Color(0xFFE89A3C))
        )
      }
    }

    // Preferred Nickname & Response Length
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Brevity & Nicknames", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = persona.preferredNickname,
          onValueChange = { onUpdatePersona(persona.copy(preferredNickname = it)) },
          label = { Text("What Ted calls you (e.g. Champ, Kid, Boss)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("nickname_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Response Length", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ResponseLength.values().forEach { len ->
            FilterChip(
              selected = persona.responseLength == len,
              onClick = { onUpdatePersona(persona.copy(responseLength = len)) },
              label = { Text(len.label.substringBefore(" ("), fontSize = 12.sp) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun VoiceTabContent(
  voice: VoiceSettings,
  availableVoices: List<String>,
  onUpdateVoice: (VoiceSettings) -> Unit,
  onPreviewVoice: (VoiceMode?) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Voice Modes Profile Selection
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth().testTag("voice_modes_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Voice Mode Profiles", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "Distinct vocal personas tailored for Ted's comedy.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Button(
            onClick = { onPreviewVoice(voice.mode) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE89A3C)),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Preview", fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(VoiceMode.values()) { mode ->
            FilterChip(
              selected = voice.mode == mode,
              onClick = {
                val updated = voice.copy(
                  mode = mode,
                  speechPitch = mode.defaultPitch,
                  speechRate = mode.defaultRate,
                  emotionalInflection = mode.defaultInflection,
                  responseCadence = mode.defaultCadence
                )
                onUpdateVoice(updated)
                onPreviewVoice(mode)
              },
              label = { Text(mode.displayName, fontSize = 12.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = voice.mode.description,
          fontSize = 12.sp,
          color = Color(0xFFE89A3C),
          fontWeight = FontWeight.Medium
        )
      }
    }

    // Fine-Grained Controls: Pitch & Rate Sliders per mode
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Fine-Grained Pitch & Speed (${voice.mode.displayName})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Pitch: ${String.format("%.2f", voice.speechPitch)}", fontSize = 13.sp)
        Slider(
          value = voice.speechPitch,
          onValueChange = { onUpdateVoice(voice.copy(speechPitch = it)) },
          valueRange = 0.65f..1.45f,
          colors = SliderDefaults.colors(thumbColor = Color(0xFFE89A3C), activeTrackColor = Color(0xFFE89A3C))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Speech Rate: ${String.format("%.2f", voice.speechRate)}x", fontSize = 13.sp)
        Slider(
          value = voice.speechRate,
          onValueChange = { onUpdateVoice(voice.copy(speechRate = it)) },
          valueRange = 0.75f..1.55f,
          colors = SliderDefaults.colors(thumbColor = Color(0xFFE89A3C), activeTrackColor = Color(0xFFE89A3C))
        )
      }
    }

    // Emotional Inflection & Response Cadence
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Emotional Inflection", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          EmotionalInflection.values().forEach { inflection ->
            FilterChip(
              selected = voice.emotionalInflection == inflection,
              onClick = { onUpdateVoice(voice.copy(emotionalInflection = inflection)) },
              label = { Text(inflection.label, fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Response Cadence & Rhythm", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ResponseCadence.values().forEach { cadence ->
            FilterChip(
              selected = voice.responseCadence == cadence,
              onClick = { onUpdateVoice(voice.copy(responseCadence = cadence)) },
              label = { Text(cadence.label.substringBefore(" ("), fontSize = 11.sp) }
            )
          }
        }
      }
    }

    // Continuous Hands-Free Voice Conversational Loop
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth().testTag("continuous_voice_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Continuous Hands-Free Conversation", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "Automated conversational loop: restarts listening after Ted speaks. Speak freely without tapping.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = voice.continuousHandsFree,
            onCheckedChange = { onUpdateVoice(voice.copy(continuousHandsFree = it, autoListenAfterResponse = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE89A3C))
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Wake Phrase Detection", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "Respond to \"Hey Ted\", \"Ted\", or \"Listen up\".",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = voice.wakeWordEnabled,
            onCheckedChange = { onUpdateVoice(voice.copy(wakeWordEnabled = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE89A3C))
          )
        }
      }
    }
  }
}
