package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TedApplication
import com.example.screen.ScreenCaptureActivity
import com.example.screen.ScreenRoastState
import com.example.ui.character.TedCharacterView
import com.example.ui.character.TedState

@Composable
fun ScreenRoastScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val app = TedApplication.instance
  val screenManager = app.screenRoastManager
  val roastState by screenManager.state.collectAsState()
  val lastBitmap by screenManager.lastCapturedBitmap.collectAsState()

  var isRoastMode by remember { mutableStateOf(true) }

  // Photo picker for picking screenshots from device
  val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val bmp = BitmapFactory.decodeStream(stream)
          if (bmp != null) {
            screenManager.analyzeProvidedBitmap(bmp, isRoastMode)
          }
        }
      } catch (_: Exception) {}
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Title & Intro Card
    ElevatedCard(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      modifier = Modifier.fillMaxWidth().testTag("screen_awareness_header")
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TedCharacterView(
          state = if (isRoastMode) TedState.ROASTING else TedState.IDLE,
          size = 64.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Screen Awareness & Roast",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Let Ted analyze what's visible on your screen.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
        }
      }
    }

    // Active Screen Sharing Banner with Stop Button
    AnimatedVisibility(visible = roastState !is ScreenRoastState.Idle) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF38231C)),
        modifier = Modifier.fillMaxWidth().testTag("active_screen_session_banner")
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFE89A3C))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Screen Sharing Session Active",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF4ECE4)
              )
              Text(
                text = "Content being analyzed by Ted",
                fontSize = 11.sp,
                color = Color(0xFFAFA299)
              )
            }
          }

          Button(
            onClick = { screenManager.stopScreenSession() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD34538)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Stop", fontSize = 12.sp)
          }
        }
      }
    }

    // Mode Selector: Savage Roast vs Neutral Explanation
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(2.dp),
      modifier = Modifier.fillMaxWidth().testTag("roast_mode_toggle_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (isRoastMode) Icons.Default.Whatshot else Icons.Default.Lightbulb,
              contentDescription = null,
              tint = if (isRoastMode) Color(0xFFE89A3C) else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = if (isRoastMode) "Savage Roast Mode 🔥" else "Neutral Explanation Mode 💡",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isRoastMode) "Witty, sarcastic burns about your screen" else "Direct, straightforward technical breakdown",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isRoastMode,
            onCheckedChange = { isRoastMode = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color(0xFFE89A3C),
              checkedTrackColor = Color(0xFF4A3C34)
            ),
            modifier = Modifier.testTag("mode_switch")
          )
        }
      }
    }

    // Action Controls: Capture Screen, Pick Screenshot, or Test Sample
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(
        onClick = {
          ScreenCaptureActivity.launch(context, isRoast = isRoastMode)
        },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isRoastMode) Color(0xFFC77A23) else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("capture_screen_button")
      ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isRoastMode) "📸 Capture & Roast Current Screen" else "📸 Capture & Explain Current Screen",
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("pick_screenshot_button")
        ) {
          Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pick Image", fontSize = 13.sp)
        }

        OutlinedButton(
          onClick = {
            // Generate a synthetic mock messy app screen with tabs & error for immediate testing
            val sampleBmp = createSampleMessyScreenBitmap()
            screenManager.analyzeProvidedBitmap(sampleBmp, isRoastMode)
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("sample_screen_button")
        ) {
          Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Test Sample", fontSize = 13.sp)
        }
      }
    }

    // Analysis Status / Result Card
    when (val state = roastState) {
      is ScreenRoastState.Capturing -> {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text("Capturing screen frame with permission...", fontSize = 14.sp)
          }
        }
      }

      is ScreenRoastState.Analyzing -> {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFE89A3C))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                text = if (state.isRoastMode) "Ted is cooking up a savage roast..." else "Ted is analyzing visible text & UI...",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
              Text("Analyzing layout, text, and visual hierarchy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      is ScreenRoastState.Success -> {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1F18)),
          elevation = CardDefaults.cardElevation(4.dp),
          modifier = Modifier.fillMaxWidth().testTag("roast_result_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 12.dp)
            ) {
              Icon(
                imageVector = if (state.isRoastMode) Icons.Default.Whatshot else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (state.isRoastMode) Color(0xFFE89A3C) else Color(0xFF5A9E6F),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (state.isRoastMode) "Ted's Screen Roast" else "Ted's Screen Explanation",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF4ECE4)
              )
              Spacer(modifier = Modifier.weight(1f))
              IconButton(
                onClick = { app.voiceManager.speak(state.resultText) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", tint = Color(0xFFE89A3C))
              }
            }

            // Screen preview thumbnail
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF4A3C34), RoundedCornerShape(12.dp))
            ) {
              Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = "Screen Capture Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ted's commentary
            Text(
              text = state.resultText,
              fontSize = 15.sp,
              lineHeight = 22.sp,
              color = Color(0xFFF4ECE4)
            )
          }
        }
      }

      is ScreenRoastState.Error -> {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0x33D34538)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD34538))
            Spacer(modifier = Modifier.width(12.dp))
            Text(state.message, color = Color(0xFFD34538), fontSize = 13.sp)
          }
        }
      }

      ScreenRoastState.Idle -> {
        // Last bitmap thumbnail if available
        if (lastBitmap != null) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text("Last Captured Frame", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Spacer(modifier = Modifier.height(8.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(130.dp)
                  .clip(RoundedCornerShape(10.dp))
              ) {
                Image(
                  bitmap = lastBitmap!!.asImageBitmap(),
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Fit
                )
              }
            }
          }
        }
      }
    }

    // Privacy & Security Disclosure Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      modifier = Modifier.fillMaxWidth().testTag("privacy_disclosure_card")
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.Top
      ) {
        Icon(
          Icons.Default.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "User-Approved Screen Privacy",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "• Capture occurs only when you tap 'Capture Screen'.\n• No continuous or secret background video recording.\n• Secure system screens & passwords are automatically protected by Android.\n• Screen analysis can be stopped at any moment.",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

/**
 * Creates a synthetic mockup screenshot of a messy browser/app screen with cluttered tabs,
 * an error notification, and a messy UI layout for testing roasts in emulator.
 */
private fun createSampleMessyScreenBitmap(): Bitmap {
  val width = 720
  val height = 1280
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  // Background
  canvas.drawColor(android.graphics.Color.rgb(30, 30, 35))

  val paint = Paint(Paint.ANTI_ALIAS_FLAG)

  // App Bar
  paint.color = android.graphics.Color.rgb(45, 45, 55)
  canvas.drawRect(0f, 0f, width.toFloat(), 140f, paint)

  paint.color = android.graphics.Color.WHITE
  paint.textSize = 34f
  canvas.drawText("Browser - 47 Tabs Open (Help)", 30f, 90f, paint)

  // Red Error banner
  paint.color = android.graphics.Color.rgb(180, 40, 40)
  canvas.drawRoundRect(30f, 170f, width - 30f, 290f, 20f, 20f, paint)

  paint.color = android.graphics.Color.WHITE
  paint.textSize = 28f
  canvas.drawText("CRITICAL ERROR 404: Layout Overwhelmed", 50f, 225f, paint)
  paint.textSize = 22f
  canvas.drawText("User has opened 14 duplicate Amazon tabs for socks.", 50f, 265f, paint)

  // Messy content blocks
  paint.color = android.graphics.Color.rgb(60, 60, 75)
  canvas.drawRoundRect(30f, 320f, width - 30f, 520f, 16f, 16f, paint)
  paint.color = android.graphics.Color.LTGRAY
  paint.textSize = 26f
  canvas.drawText("Shopping Cart: $1,429.00 (Unpaid for 3 months)", 50f, 380f, paint)
  canvas.drawText("Battery: 2% (Phone is crying)", 50f, 440f, paint)

  // Notification clutter
  paint.color = android.graphics.Color.rgb(50, 70, 90)
  canvas.drawRoundRect(30f, 550f, width - 30f, 750f, 16f, 16f, paint)
  paint.color = android.graphics.Color.WHITE
  canvas.drawText("Notifications: 843 Unread Messages", 50f, 620f, paint)
  canvas.drawText("Reminder: Clean up your digital life tomorrow", 50f, 680f, paint)

  return bitmap
}
