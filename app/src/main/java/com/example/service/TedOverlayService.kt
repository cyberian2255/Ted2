package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.TedApplication
import com.example.screen.ScreenCaptureActivity
import com.example.ui.character.TedCharacterView
import com.example.ui.character.TedState
import com.example.voice.VoiceState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TedOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

  companion object {
    private const val CHANNEL_ID = "ted_overlay_channel"
    private const val NOTIFICATION_ID = 404

    fun start(context: Context) {
      val intent = Intent(context, TedOverlayService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, TedOverlayService::class.java))
    }
  }

  private val lock = Any()
  private var windowManager: WindowManager? = null
  private var overlayView: View? = null
  private var isViewAttached = false
  private var windowLayoutParams: WindowManager.LayoutParams? = null

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val savedStateRegistryController = SavedStateRegistryController.create(this)
  private val appViewModelStore = ViewModelStore()

  override val lifecycle: Lifecycle get() = lifecycleRegistry
  override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
  override val viewModelStore: ViewModelStore get() = appViewModelStore

  private var isExpanded by mutableStateOf(false)

  override fun onCreate() {
    super.onCreate()
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.currentState = Lifecycle.State.CREATED
    startForegroundNotification()
    initOverlayView()
    lifecycleRegistry.currentState = Lifecycle.State.RESUMED
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // START_STICKY ensures the service restarts if terminated by memory pressure during multi-tasking
    return START_STICKY
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Adjust coordinates on rotation / configuration shift so overlay stays on screen
    synchronized(lock) {
      val params = windowLayoutParams ?: return
      val wm = windowManager ?: return
      val view = overlayView ?: return
      if (!isViewAttached) return

      try {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        val maxX = max(0, metrics.widthPixels - 180)
        val maxY = max(0, metrics.heightPixels - 200)

        params.x = min(max(0, params.x), maxX)
        params.y = min(max(0, params.y), maxY)
        wm.updateViewLayout(view, params)
      } catch (_: Exception) {}
    }
  }

  private fun startForegroundNotification() {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val chan = NotificationChannel(
        CHANNEL_ID,
        "Ted Floating Companion",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows when Ted floating companion is active over apps"
      }
      nm.createNotificationChannel(chan)
    }

    val openIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      },
      PendingIntent.FLAG_IMMUTABLE
    )

    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Ted AI Companion")
      .setContentText("Ted is hovering above your apps. Tap to chat or roast.")
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentIntent(openIntent)
      .setOngoing(true)
      .build()

    startForeground(NOTIFICATION_ID, notification)
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun initOverlayView() {
    synchronized(lock) {
      if (isViewAttached) return
      try {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val wm = windowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
          @Suppress("DEPRECATION")
          WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
          WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.WRAP_CONTENT,
          layoutType,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
          PixelFormat.TRANSLUCENT
        ).apply {
          gravity = Gravity.TOP or Gravity.START
          x = 80
          y = 300
        }
        windowLayoutParams = params

        val composeView = ComposeView(this).apply {
          setViewTreeLifecycleOwner(this@TedOverlayService)
          setViewTreeSavedStateRegistryOwner(this@TedOverlayService)
          setViewTreeViewModelStoreOwner(this@TedOverlayService)
          setContent {
            OverlayContent(
              onDismiss = { stopSelf() },
              onOpenApp = {
                val i = Intent(this@TedOverlayService, MainActivity::class.java).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(i)
              },
              onRoastScreen = {
                ScreenCaptureActivity.launch(this@TedOverlayService, isRoast = true)
              }
            )
          }
        }

        // Drag handling with screen clamping to prevent disappearing and tap detection
        composeView.setOnTouchListener(object : View.OnTouchListener {
          private var initialX = 0
          private var initialY = 0
          private var initialTouchX = 0f
          private var initialTouchY = 0f
          private var isDragging = false
          private var downTime = 0L

          override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event == null) return false
            when (event.action) {
              MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                downTime = System.currentTimeMillis()
                return false
              }
              MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (abs(dx) > 10 || abs(dy) > 10) {
                  isDragging = true
                  val metrics = DisplayMetrics()
                  @Suppress("DEPRECATION")
                  wm.defaultDisplay.getMetrics(metrics)
                  val maxX = max(0, metrics.widthPixels - (v?.width ?: 120))
                  val maxY = max(0, metrics.heightPixels - (v?.height ?: 120))

                  params.x = min(max(0, (initialX + dx).toInt()), maxX)
                  params.y = min(max(0, (initialY + dy).toInt()), maxY)

                  try {
                    wm.updateViewLayout(composeView, params)
                  } catch (_: Exception) {}
                  return true
                }
              }
              MotionEvent.ACTION_UP -> {
                val dx = abs(event.rawX - initialTouchX)
                val dy = abs(event.rawY - initialTouchY)
                val duration = System.currentTimeMillis() - downTime
                if (!isDragging && dx < 10 && dy < 10 && duration < 500) {
                  val app = application as? TedApplication
                  if (app?.voiceManager?.isTedSpeaking == true) {
                    // Tap Face to Shut Up (Manual Interruption)
                    app.voiceManager.shutUpAndListen()
                    return true
                  }
                }
              }
            }
            return false
          }
        })

        overlayView = composeView
        wm.addView(composeView, params)
        isViewAttached = true
      } catch (_: Exception) {
        isViewAttached = false
      }
    }
  }

  @Composable
  private fun OverlayContent(
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onRoastScreen: () -> Unit
  ) {
    val app = application as TedApplication
    val voiceState by app.voiceManager.voiceState.collectAsState()
    val isTedSpeaking by app.voiceManager.isTedSpeakingFlow.collectAsState()

    val tedCharState = when {
      isTedSpeaking || voiceState == VoiceState.SPEAKING -> TedState.SPEAKING
      voiceState == VoiceState.LISTENING -> TedState.LISTENING
      voiceState == VoiceState.PROCESSING -> TedState.PROCESSING
      else -> TedState.IDLE
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(6.dp)
    ) {
      if (isExpanded) {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xF2231B16)),
          elevation = CardDefaults.cardElevation(8.dp),
          modifier = Modifier.width(220.dp).padding(bottom = 8.dp)
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              modifier = Modifier.padding(bottom = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Ted Companion",
                color = Color(0xFFEAD8C7),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = { isExpanded = false }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Minimize", tint = Color(0xFFAFA299))
              }
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(vertical = 4.dp)
            ) {
              // Mic / Barge-in toggle
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
                  .size(44.dp)
                  .background(
                    when {
                      app.voiceManager.isTedSpeaking || voiceState == VoiceState.SPEAKING -> Color(0xFFD34538)
                      voiceState == VoiceState.LISTENING -> Color(0xFFE89A3C)
                      else -> Color(0xFF332720)
                    },
                    CircleShape
                  )
              ) {
                Icon(
                  imageVector = if (app.voiceManager.isTedSpeaking || voiceState == VoiceState.SPEAKING) Icons.Default.Stop else Icons.Default.Mic,
                  contentDescription = "Voice Mic",
                  tint = Color(0xFFEAD8C7)
                )
              }

              // Screen Roast
              IconButton(
                onClick = {
                  isExpanded = false
                  onRoastScreen()
                },
                modifier = Modifier
                  .size(44.dp)
                  .background(Color(0xFFE89A3C), CircleShape)
              ) {
                Icon(
                  Icons.Default.PhotoCamera,
                  contentDescription = "Screen Roast",
                  tint = Color(0xFF19120E)
                )
              }

              // Open Full App
              IconButton(
                onClick = {
                  isExpanded = false
                  onOpenApp()
                },
                modifier = Modifier
                  .size(44.dp)
                  .background(Color(0xFF332720), CircleShape)
              ) {
                Icon(
                  Icons.Default.OpenInFull,
                  contentDescription = "Open App",
                  tint = Color(0xFFEAD8C7)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = when {
                app.voiceManager.isTedSpeaking || voiceState == VoiceState.SPEAKING -> "Tap Ted's face to shut him up"
                voiceState == VoiceState.LISTENING -> "Ted is listening..."
                voiceState == VoiceState.PROCESSING -> "Ted is thinking..."
                else -> "Tap 📸 to roast any app screen"
              },
              color = Color(0xFFAFA299),
              fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Hide Overlay",
              color = Color(0xFFD34538),
              fontSize = 11.sp,
              modifier = Modifier
                .clickable { onDismiss() }
                .padding(4.dp)
            )
          }
        }
      }

      // Compact Draggable Bubble (Ted's Face Icon)
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(Color(0xEE231B16))
          .clickable {
            if (app.voiceManager.isTedSpeaking) {
              // Tap Face to Shut Up (Manual Interruption):
              // Immediately stops TTS, sets isTedSpeaking = false, and restarts microphone listening
              app.voiceManager.shutUpAndListen()
            } else {
              isExpanded = !isExpanded
            }
          }
          .padding(4.dp),
        contentAlignment = Alignment.Center
      ) {
        TedCharacterView(
          state = tedCharState,
          size = 52.dp,
          modifier = Modifier.clickable {
            if (app.voiceManager.isTedSpeaking) {
              app.voiceManager.shutUpAndListen()
            } else {
              isExpanded = !isExpanded
            }
          }
        )
      }
    }
  }

  override fun onDestroy() {
    lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    synchronized(lock) {
      if (isViewAttached && overlayView != null) {
        try {
          windowManager?.removeView(overlayView)
        } catch (_: Exception) {}
        overlayView = null
        isViewAttached = false
      }
    }
    appViewModelStore.clear()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
