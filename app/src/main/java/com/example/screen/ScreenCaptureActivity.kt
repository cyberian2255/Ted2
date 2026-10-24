package com.example.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ScreenCaptureActivity : Activity() {

  companion object {
    private const val REQUEST_MEDIA_PROJECTION = 991
    private const val EXTRA_IS_ROAST = "extra_is_roast"

    fun launch(context: Context, isRoast: Boolean) {
      val intent = Intent(context, ScreenCaptureActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(EXTRA_IS_ROAST, isRoast)
      }
      context.startActivity(intent)
    }
  }

  private var isRoast: Boolean = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isRoast = intent.getBooleanExtra(EXTRA_IS_ROAST, true)

    val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    if (mgr != null) {
      @Suppress("DEPRECATION")
      startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    } else {
      finish()
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
      // In Android 14+ (API 34+), getMediaProjection must be called while a foreground service
      // of type FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION is running.
      TedScreenCaptureService.startCapture(this, resultCode, data, isRoast)
    }
    finish()
  }
}
