package com.example.screen

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.TedApplication

class TedScreenCaptureService : Service() {

  companion object {
    private const val CHANNEL_ID = "ted_screen_capture_channel"
    private const val NOTIFICATION_ID = 505
    private const val EXTRA_RESULT_CODE = "extra_result_code"
    private const val EXTRA_DATA = "extra_data"
    private const val EXTRA_IS_ROAST = "extra_is_roast"

    fun startCapture(context: Context, resultCode: Int, data: Intent, isRoast: Boolean) {
      val intent = Intent(context, TedScreenCaptureService::class.java).apply {
        putExtra(EXTRA_RESULT_CODE, resultCode)
        putExtra(EXTRA_DATA, data)
        putExtra(EXTRA_IS_ROAST, isRoast)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForegroundNotification()

    val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
    @Suppress("DEPRECATION")
    val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
    val isRoast = intent?.getBooleanExtra(EXTRA_IS_ROAST, true) ?: true

    if (resultCode == Activity.RESULT_OK && data != null) {
      val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
      try {
        val projection = mgr?.getMediaProjection(resultCode, data)
        if (projection != null) {
          val app = application as TedApplication
          app.screenRoastManager.performCaptureWithProjection(projection, isRoast) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
          }
          return START_NOT_STICKY
        }
      } catch (_: Exception) {}
    }

    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    return START_NOT_STICKY
  }

  private fun startForegroundNotification() {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val chan = NotificationChannel(
        CHANNEL_ID,
        "Ted Screen Capture",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Active when Ted is capturing the screen for roast analysis"
      }
      nm.createNotificationChannel(chan)
    }

    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Ted Screen Analysis")
      .setContentText("Capturing visible screen frame for Ted...")
      .setSmallIcon(android.R.drawable.ic_menu_camera)
      .setOngoing(true)
      .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
