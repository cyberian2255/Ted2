package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TedApplication
import com.example.service.TedOverlayService

@Composable
fun SettingsDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val app = TedApplication.instance
  val currentKey by app.preferences.apiKey.collectAsState()
  val currentModel by app.preferences.aiModel.collectAsState()

  var keyInput by remember { mutableStateOf(currentKey) }
  var overlayEnabled by remember {
    mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("App & API Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Floating Companion Overlay Permission & Toggle
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Floating Overlay", fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
              Switch(
                checked = overlayEnabled,
                onCheckedChange = { enable ->
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                      Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                  } else {
                    overlayEnabled = enable
                    if (enable) {
                      TedOverlayService.start(context)
                    } else {
                      TedOverlayService.stop(context)
                    }
                  }
                }
              )
            }
            Text(
              text = "Keep Ted's draggable bubble accessible above other apps for quick screen roasts and talk.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // API Key Section
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("API Key (OpenRouter / AI)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = keyInput,
              onValueChange = { keyInput = it },
              placeholder = { Text("sk-or-v1-...", fontSize = 12.sp) },
              maxLines = 2,
              modifier = Modifier.fillMaxWidth().testTag("api_key_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = {
                app.preferences.updateApiKey(keyInput)
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.align(Alignment.End)
            ) {
              Text("Update Key", fontSize = 12.sp)
            }
          }
        }

        // Security Warning Card (Exact mandate)
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0x33D34538))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD34538), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Security Warning",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD34538),
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}
