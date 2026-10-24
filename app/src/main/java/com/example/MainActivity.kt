package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.ActionsScreen
import com.example.ui.screens.CompanionScreen
import com.example.ui.screens.MemoriesScreen
import com.example.ui.screens.PersonaVoiceScreen
import com.example.ui.screens.ScreenRoastScreen
import com.example.ui.theme.TedTheme
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
  COMPANION("Ted", Icons.Default.ChatBubble, "tab_companion"),
  SCREEN_ROAST("Screen", Icons.Default.PhotoCamera, "tab_screen_roast"),
  ACTIONS("Actions", Icons.Default.Widgets, "tab_actions"),
  PERSONA("Persona", Icons.Default.Tune, "tab_persona"),
  MEMORIES("Memories", Icons.Default.Psychology, "tab_memories")
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      TedTheme {
        TedAppMainContent()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TedAppMainContent() {
  val context = LocalContext.current
  val app = TedApplication.instance
  val scope = rememberCoroutineScope()

  var currentTab by remember { mutableStateOf(AppTab.COMPANION) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  // Permission launcher for audio and notifications
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ -> }

  LaunchedEffect(Unit) {
    val neededPermissions = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      neededPermissions.add(Manifest.permission.RECORD_AUDIO)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (neededPermissions.isNotEmpty()) {
      permissionLauncher.launch(neededPermissions.toTypedArray())
    }
  }

  // Handle Android Back button: pop to Companion tab first
  BackHandler(enabled = currentTab != AppTab.COMPANION) {
    currentTab = AppTab.COMPANION
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (currentTab) {
              AppTab.COMPANION -> "Ted AI Companion"
              AppTab.SCREEN_ROAST -> "Screen Awareness"
              AppTab.ACTIONS -> "Android Actions"
              AppTab.PERSONA -> "Persona & Voice"
              AppTab.MEMORIES -> "Personalization"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        },
        actions = {
          if (currentTab == AppTab.COMPANION) {
            IconButton(
              onClick = { scope.launch { app.repository.clearChat() } },
              modifier = Modifier.testTag("clear_chat_button")
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Conversation")
            }
          }
          IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
          titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("bottom_nav_bar")
      ) {
        AppTab.values().forEach { tab ->
          val selected = currentTab == tab
          NavigationBarItem(
            selected = selected,
            onClick = { currentTab = tab },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(22.dp)
              )
            },
            label = {
              Text(
                text = tab.title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color(0xFF19120E),
              indicatorColor = Color(0xFFE89A3C)
            ),
            modifier = Modifier.testTag(tab.tag)
          )
        }
      }
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    when (currentTab) {
      AppTab.COMPANION -> {
        CompanionScreen(
          onNavigateToScreenRoast = { currentTab = AppTab.SCREEN_ROAST },
          modifier = Modifier.padding(innerPadding)
        )
      }
      AppTab.SCREEN_ROAST -> {
        ScreenRoastScreen(modifier = Modifier.padding(innerPadding))
      }
      AppTab.ACTIONS -> {
        ActionsScreen(modifier = Modifier.padding(innerPadding))
      }
      AppTab.PERSONA -> {
        PersonaVoiceScreen(modifier = Modifier.padding(innerPadding))
      }
      AppTab.MEMORIES -> {
        MemoriesScreen(modifier = Modifier.padding(innerPadding))
      }
    }

    if (showSettingsDialog) {
      SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
  }
}
