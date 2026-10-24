package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TedApplication
import com.example.actions.AssistantActionResult

@Composable
fun ActionsScreen(modifier: Modifier = Modifier) {
  val app = TedApplication.instance
  val actions = app.actionManager

  var lastActionResult by remember { mutableStateOf<AssistantActionResult?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var customTimerSeconds by remember { mutableStateOf("60") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    ElevatedCard(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      ),
      modifier = Modifier.fillMaxWidth().testTag("actions_header_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Android Assistant Actions",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Native Android utilities inspired by the legacy assistant archive.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }
    }

    // Speech feedback card from Ted
    if (lastActionResult != null) {
      val res = lastActionResult!!
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1F17)),
        modifier = Modifier.fillMaxWidth().testTag("action_result_card")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = res.title,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color(0xFFE89A3C)
            )
            IconButton(
              onClick = { app.voiceManager.speak(res.speechSummary) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = Color(0xFFE89A3C))
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = res.speechSummary,
            fontSize = 14.sp,
            color = Color(0xFFF4ECE4)
          )
        }
      }
    }

    // Quick Status Queries (Time, Battery, Wifi)
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Device & System Telemetry", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ActionTile(
            icon = Icons.Default.AccessTime,
            label = "Time & Date",
            modifier = Modifier.weight(1f),
            onClick = {
              val r = actions.getTimeAndDate()
              lastActionResult = r
              app.voiceManager.speak(r.speechSummary)
            }
          )
          ActionTile(
            icon = Icons.Default.BatteryChargingFull,
            label = "Battery",
            modifier = Modifier.weight(1f),
            onClick = {
              val r = actions.getBatteryStatus()
              lastActionResult = r
              app.voiceManager.speak(r.speechSummary)
            }
          )
          ActionTile(
            icon = Icons.Default.Wifi,
            label = "Network",
            modifier = Modifier.weight(1f),
            onClick = {
              val r = actions.checkNetworkConnectivity()
              lastActionResult = r
              app.voiceManager.speak(r.speechSummary)
            }
          )
        }
      }
    }

    // Timer & Alarm Section
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Timers & Alarms", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(30 to "30s", 60 to "1 min", 300 to "5 min", 600 to "10 min").forEach { (sec, lbl) ->
            OutlinedButton(
              onClick = {
                val r = actions.setTimer(sec, "Ted Timer $lbl")
                lastActionResult = r
                app.voiceManager.speak(r.speechSummary)
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(lbl, fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = customTimerSeconds,
            onValueChange = { customTimerSeconds = it.filter { c -> c.isDigit() } },
            label = { Text("Custom Seconds", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val sec = customTimerSeconds.toIntOrNull() ?: 60
              val r = actions.setTimer(sec, "Ted Custom Timer")
              lastActionResult = r
              app.voiceManager.speak(r.speechSummary)
            },
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Set Timer")
          }
        }
      }
    }

    // Web Search & Online Portals
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Web Lookup & Sites", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Google, wiki...", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (searchQuery.isNotBlank()) {
                val r = actions.performWebSearch(searchQuery.trim())
                lastActionResult = r
                app.voiceManager.speak(r.speechSummary)
              }
            },
            modifier = Modifier
              .size(48.dp)
              .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
          ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onPrimary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick web shortcuts
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            "Reddit" to "https://www.reddit.com",
            "YouTube" to "https://www.youtube.com",
            "Wiki" to "https://en.wikipedia.org",
            "GitHub" to "https://github.com"
          ).forEach { (site, url) ->
            OutlinedButton(
              onClick = {
                val r = actions.openWebsite(url)
                lastActionResult = r
                app.voiceManager.speak(r.speechSummary)
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(site, fontSize = 11.sp)
            }
          }
        }
      }
    }

    // Universal App Launcher
    var appSearchQuery by remember { mutableStateOf("") }
    val installedApps = remember { actions.getInstalledLaunchableApps() }
    val filteredApps = remember(appSearchQuery, installedApps) {
      if (appSearchQuery.isBlank()) installedApps else installedApps.filter {
        it.appName.contains(appSearchQuery, ignoreCase = true) || it.packageName.contains(appSearchQuery, ignoreCase = true)
      }
    }

    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth().testTag("universal_app_launcher_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Universal App Launcher", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(
          text = "Scan and open ANY installed app on this device by tap or voice command (\"Ted, open Spotify\").",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = appSearchQuery,
          onValueChange = { appSearchQuery = it },
          placeholder = { Text("Filter ${installedApps.size} installed apps...", fontSize = 13.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("app_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Display filtered installed apps list
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())
        ) {
          if (filteredApps.isEmpty()) {
            Text(
              "No installed app matching '$appSearchQuery'",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(8.dp)
            )
          } else {
            filteredApps.take(15).forEach { appInfo ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                  .clickable {
                    val r = actions.launchApp(appInfo.packageName, appInfo.appName)
                    lastActionResult = r
                    app.voiceManager.speak(r.speechSummary)
                  }
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(appInfo.appName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                  Text(appInfo.packageName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Open", fontSize = 12.sp, color = Color(0xFFE89A3C), fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ActionTile(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  OutlinedButton(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    modifier = modifier.height(64.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(label, fontSize = 11.sp, maxLines = 1)
    }
  }
}
