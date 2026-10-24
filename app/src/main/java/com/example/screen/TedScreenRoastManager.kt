package com.example.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.ai.AiResponse
import com.example.ai.TedAiEngine
import com.example.data.preferences.TedPreferences
import com.example.data.repository.TedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ScreenRoastState {
  object Idle : ScreenRoastState()
  object Capturing : ScreenRoastState()
  data class Analyzing(val bitmap: Bitmap, val isRoastMode: Boolean) : ScreenRoastState()
  data class Success(val bitmap: Bitmap, val resultText: String, val isRoastMode: Boolean) : ScreenRoastState()
  data class Error(val message: String) : ScreenRoastState()
}

class TedScreenRoastManager(
  private val context: Context,
  private val aiEngine: TedAiEngine,
  private val repository: TedRepository,
  private val preferences: TedPreferences,
  private val scope: CoroutineScope
) {
  private val _state = MutableStateFlow<ScreenRoastState>(ScreenRoastState.Idle)
  val state: StateFlow<ScreenRoastState> = _state.asStateFlow()

  private val _lastCapturedBitmap = MutableStateFlow<Bitmap?>(null)
  val lastCapturedBitmap: StateFlow<Bitmap?> = _lastCapturedBitmap.asStateFlow()

  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null

  fun setMediaProjection(projection: MediaProjection) {
    this.mediaProjection = projection
  }

  fun stopScreenSession() {
    try {
      virtualDisplay?.release()
      virtualDisplay = null
      imageReader?.close()
      imageReader = null
      mediaProjection?.stop()
      mediaProjection = null
    } catch (_: Exception) {}
    _state.value = ScreenRoastState.Idle
  }

  fun analyzeProvidedBitmap(bitmap: Bitmap, isRoastMode: Boolean) {
    _lastCapturedBitmap.value = bitmap
    _state.value = ScreenRoastState.Analyzing(bitmap, isRoastMode)

    scope.launch {
      val prompt = if (isRoastMode) {
        "Roast what is visible on this screen. Be witty, sarcastic, and hilarious about the layout, cluttered UI, messy text, or buttons. Keep it under 3 short sentences."
      } else {
        "Give a clear, neutral, straightforward explanation of what is on this screen, including any visible text, key buttons, or errors. No sarcasm."
      }

      val response = aiEngine.generateReply(
        userMessage = prompt,
        screenBitmap = bitmap,
        isScreenRoast = isRoastMode
      )

      val resultText = when (response) {
        is AiResponse.Success -> response.text
        is AiResponse.Fallback -> "${response.text} (Note: ${response.errorDetail})"
      }

      // Save into chat history
      repository.insertMessage(
        sender = "USER",
        text = if (isRoastMode) "📸 [Screen Roast Requested]" else "📸 [Screen Analysis Requested]",
        isRoast = isRoastMode
      )
      repository.insertMessage(
        sender = "TED",
        text = resultText,
        isRoast = isRoastMode
      )

      _state.value = ScreenRoastState.Success(bitmap, resultText, isRoastMode)
    }
  }

  /**
   * Captures the screen via the system-approved MediaProjection service flow.
   */
  fun captureScreen(isRoastMode: Boolean, onNeedsPermission: () -> Unit = {}) {
    ScreenCaptureActivity.launch(context, isRoastMode)
  }

  /**
   * Performs frame extraction inside the active MediaProjection foreground service.
   */
  fun performCaptureWithProjection(
    projection: MediaProjection,
    isRoastMode: Boolean,
    onComplete: () -> Unit
  ) {
    _state.value = ScreenRoastState.Capturing

    scope.launch(Dispatchers.Default) {
      try {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        val display = projection.createVirtualDisplay(
          "TedScreenCapture",
          width,
          height,
          density,
          DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
          reader.surface,
          null,
          null
        )
        virtualDisplay = display

        // Wait a brief moment for frame to arrive
        delay(400)

        var capturedBitmap: Bitmap? = null
        val image = reader.acquireLatestImage()
        if (image != null) {
          try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bmp = Bitmap.createBitmap(
              width + rowPadding / pixelStride,
              height,
              Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buffer)
            capturedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height)
          } finally {
            image.close()
          }
        }

        // Clean up temporary capture resources immediately to prevent memory leaks
        try {
          display?.release()
          virtualDisplay = null
          reader.close()
          imageReader = null
          projection.stop()
        } catch (_: Exception) {}

        withContext(Dispatchers.Main) {
          onComplete()
          if (capturedBitmap != null) {
            analyzeProvidedBitmap(capturedBitmap, isRoastMode)
          } else {
            _state.value = ScreenRoastState.Error("No screen frame received. Please try again.")
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          onComplete()
          _state.value = ScreenRoastState.Error("Capture failed: ${e.localizedMessage}")
        }
      }
    }
  }

  fun reset() {
    _state.value = ScreenRoastState.Idle
  }
}
