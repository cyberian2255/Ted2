package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TedApplication
import com.example.data.db.ChatMessageEntity
import com.example.ui.character.TedCharacterView
import com.example.ui.character.TedState
import com.example.voice.VoiceState

@Composable
fun CompanionScreen(
  onNavigateToScreenRoast: () -> Unit,
  modifier: Modifier = Modifier
) {
  val app = TedApplication.instance
  val messages by app.repository.allMessages.collectAsState(initial = emptyList())
  val voiceState by app.voiceManager.voiceState.collectAsState()
  val isTedSpeaking by app.voiceManager.isTedSpeakingFlow.collectAsState()
  val lastTranscript by app.voiceManager.lastTranscript.collectAsState()
  val persona by app.preferences.personaSettings.collectAsState()
  val voiceSettings by app.preferences.voiceSettings.collectAsState()

  var textInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  val tedCharState = when {
    isTedSpeaking || voiceState == VoiceState.SPEAKING -> TedState.SPEAKING
    voiceState == VoiceState.LISTENING -> TedState.LISTENING
    voiceState == VoiceState.PROCESSING -> TedState.PROCESSING
    else -> if (messages.lastOrNull()?.isRoast == true) TedState.ROASTING else TedState.IDLE
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header & Animated Ted Hero Card
    ElevatedCard(
      shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag("ted_hero_card")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
      ) {
        // Ted Character (Tap Face to Shut Up)
        TedCharacterView(
          modifier = Modifier
            .clip(CircleShape)
            .clickable {
              if (app.voiceManager.isTedSpeaking || voiceState == VoiceState.SPEAKING) {
                app.voiceManager.shutUpAndListen()
              }
            },
          state = tedCharState,
          size = 140.dp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Status Badge & Wake word tag
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(
                when {
                  isTedSpeaking || voiceState == VoiceState.SPEAKING -> Color(0xFFD34538)
                  voiceState == VoiceState.LISTENING -> Color(0xFFE89A3C)
                  voiceState == VoiceState.PROCESSING -> Color(0xFFC77A23)
                  else -> Color(0xFF4A3C34)
                }
              )
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = when {
                isTedSpeaking || voiceState == VoiceState.SPEAKING -> "Ted is talking (tap face to interrupt)"
                voiceState == VoiceState.LISTENING -> "Listening..."
                voiceState == VoiceState.PROCESSING -> "Thinking..."
                else -> "Ted (${persona.roastIntensity.levelName} Roast)"
              },
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )
          }

          if (voiceSettings.wakeWordEnabled) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33E89A3C))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Wake: 'Hey Ted'",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0x285A9E6F))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = voiceSettings.mode.displayName,
              fontSize = 11.sp,
              color = Color(0xFF5A9E6F),
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Live Transcript display if listening
        AnimatedVisibility(visible = lastTranscript.isNotBlank() && voiceState == VoiceState.LISTENING) {
          Text(
            text = "\"$lastTranscript\"",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
          )
        }
      }
    }

    // Quick conversation suggestion chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      val chips = listOf(
        "Roast my habits 🔥" to "Roast my phone habits and screen time.",
        "Check battery 🔋" to "Check my battery status.",
        "Tell me a joke 😂" to "Tell me your best joke.",
        "Roast this screen 📸" to "SCREEN_ROAST",
        "Explain an error 💡" to "Help me fix a weird bug I'm seeing."
      )

      items(chips) { (label, actionText) ->
        SuggestionChip(
          onClick = {
            if (actionText == "SCREEN_ROAST") {
              onNavigateToScreenRoast()
            } else {
              app.processVoiceInput(actionText)
            }
          },
          label = { Text(label, fontSize = 12.sp) },
          colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
          ),
          border = null
        )
      }
    }

    // Chat Conversation List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 8.dp)
    ) {
      if (messages.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.Hearing,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Say \"Hey Ted\" or tap the mic below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
              )
              Text(
                text = "Ask for real help, or prepare to get roasted.",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp
              )
            }
          }
        }
      }

      items(messages, key = { it.id }) { message ->
        ChatMessageBubble(
          message = message,
          onReplayVoice = { text -> app.voiceManager.speak(text) }
        )
      }
    }

    // Bottom Input & Voice Control Bar
    Card(
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        // Voice Mic / Stop Speaking button
        IconButton(
          onClick = {
            if (app.voiceManager.isTedSpeaking || voiceState == VoiceState.SPEAKING) {
              app.voiceManager.shutUpAndListen()
            } else if (voiceState == VoiceState.LISTENING) {
              app.voiceManager.stopListening()
            } else {
              app.voiceManager.startListening()
            }
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
              when {
                app.voiceManager.isTedSpeaking || voiceState == VoiceState.SPEAKING -> Color(0xFFD34538)
                voiceState == VoiceState.LISTENING -> Color(0xFFE89A3C)
                else -> MaterialTheme.colorScheme.primaryContainer
              }
            )
            .testTag("mic_toggle_button")
        ) {
          Icon(
            imageVector = when {
              app.voiceManager.isTedSpeaking || voiceState == VoiceState.SPEAKING -> Icons.Default.Stop
              voiceState == VoiceState.LISTENING -> Icons.Default.MicOff
              else -> Icons.Default.Mic
            },
            contentDescription = "Voice toggle",
            tint = if (voiceState == VoiceState.LISTENING || isTedSpeaking || voiceState == VoiceState.SPEAKING)
              Color.White else MaterialTheme.colorScheme.onPrimaryContainer
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Screen Roast Quick Launch
        IconButton(
          onClick = onNavigateToScreenRoast,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("screen_roast_nav_button")
        ) {
          Icon(
            Icons.Default.PhotoCamera,
            contentDescription = "Roast Screen",
            tint = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Text input field
        OutlinedTextField(
          value = textInput,
          onValueChange = { textInput = it },
          placeholder = { Text("Ask or talk to Ted...", fontSize = 14.sp) },
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
          ),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input_field")
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send button
        IconButton(
          onClick = {
            if (textInput.isNotBlank()) {
              val msg = textInput.trim()
              textInput = ""
              app.processVoiceInput(msg)
            }
          },
          enabled = textInput.isNotBlank(),
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
              if (textInput.isNotBlank()) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            )
            .testTag("send_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (textInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
          )
        }
      }
    }
  }
}

@Composable
fun ChatMessageBubble(
  message: ChatMessageEntity,
  onReplayVoice: (String) -> Unit
) {
  val isUser = message.sender == "USER"

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    if (!isUser) {
      // Small Ted Avatar
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        TedCharacterView(
          state = if (message.isRoast) TedState.ROASTING else TedState.IDLE,
          size = 30.dp
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }

    Card(
      shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp
      ),
      colors = CardDefaults.cardColors(
        containerColor = if (isUser) {
          MaterialTheme.colorScheme.primary
        } else if (message.isRoast) {
          Color(0xFF3B2418) // Rich toasted espresso
        } else {
          MaterialTheme.colorScheme.surfaceVariant
        }
      ),
      modifier = Modifier
        .widthIn(max = 290.dp)
        .testTag("chat_bubble_${message.id}")
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        if (!isUser && message.isRoast) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
          ) {
            Icon(
              Icons.Default.Whatshot,
              contentDescription = "Roast",
              tint = Color(0xFFE89A3C),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Ted's Roast",
              color = Color(0xFFE89A3C),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Text(
          text = message.text,
          color = if (isUser) MaterialTheme.colorScheme.onPrimary
          else Color(0xFFF4ECE4),
          fontSize = 14.sp,
          lineHeight = 20.sp
        )

        if (!isUser) {
          Row(
            modifier = Modifier
              .align(Alignment.End)
              .padding(top = 4.dp)
              .clickable { onReplayVoice(message.text) },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Listen",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Listen",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }
      }
    }
  }
}
