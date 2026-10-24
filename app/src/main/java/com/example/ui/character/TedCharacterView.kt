package com.example.ui.character

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TedState {
  IDLE,
  LISTENING,
  PROCESSING,
  SPEAKING,
  ROASTING
}

@Composable
fun TedCharacterView(
  modifier: Modifier = Modifier,
  state: TedState = TedState.IDLE,
  size: Dp = 160.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "ted_animation")

  // Subtle breathing animation
  val breathScale by infiniteTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.02f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "breath"
  )

  // Speaking mouth movement
  val mouthOpen by infiniteTransition.animateFloat(
    initialValue = 0.1f,
    targetValue = if (state == TedState.SPEAKING) 1.0f else 0.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(if (state == TedState.SPEAKING) 140 else 1800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "mouth"
  )

  // Listening pulse ring
  val listeningPulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "listening_pulse"
  )

  // Processing eye glance
  val glanceOffset by infiniteTransition.animateFloat(
    initialValue = -3f,
    targetValue = 3f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glance"
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height
      val scale = (w / 160f) * if (state == TedState.IDLE) breathScale else 1f

      // Draw listening calm halo
      if (state == TedState.LISTENING) {
        drawCircle(
          color = Color(0x33E89A3C),
          radius = (w * 0.46f) * listeningPulse,
          center = Offset(w / 2, h / 2)
        )
        drawCircle(
          color = Color(0x22E89A3C),
          radius = (w * 0.52f) * listeningPulse,
          center = Offset(w / 2, h / 2)
        )
      } else if (state == TedState.ROASTING) {
        drawCircle(
          color = Color(0x2BD34538),
          radius = (w * 0.48f) * breathScale,
          center = Offset(w / 2, h / 2)
        )
      }

      val centerX = w / 2f
      val centerY = h / 2f

      // Bear Outer Ears
      val earRadius = 20f * scale
      val innerEarRadius = 12f * scale
      val leftEarCenter = Offset(centerX - 42f * scale, centerY - 38f * scale)
      val rightEarCenter = Offset(centerX + 42f * scale, centerY - 38f * scale)

      // Outer ears
      drawCircle(color = Color(0xFFBA6B28), radius = earRadius, center = leftEarCenter)
      drawCircle(color = Color(0xFFBA6B28), radius = earRadius, center = rightEarCenter)

      // Inner ears
      drawCircle(color = Color(0xFFE8A870), radius = innerEarRadius, center = leftEarCenter)
      drawCircle(color = Color(0xFFE8A870), radius = innerEarRadius, center = rightEarCenter)

      // Bear Head Base
      val headRadius = 48f * scale
      drawCircle(
        color = Color(0xFFC97834),
        radius = headRadius,
        center = Offset(centerX, centerY - 2f * scale)
      )

      // Muzzle (Snout)
      val muzzleWidth = 44f * scale
      val muzzleHeight = 32f * scale
      val muzzleCenterY = centerY + 10f * scale
      drawRoundRect(
        color = Color(0xFFF4D3B2),
        topLeft = Offset(centerX - muzzleWidth / 2f, muzzleCenterY - muzzleHeight / 2f),
        size = Size(muzzleWidth, muzzleHeight),
        cornerRadius = CornerRadius(16f * scale, 14f * scale)
      )

      // Nose
      val noseWidth = 16f * scale
      val noseHeight = 11f * scale
      val noseCenterY = muzzleCenterY - 6f * scale
      drawRoundRect(
        color = Color(0xFF241810),
        topLeft = Offset(centerX - noseWidth / 2f, noseCenterY - noseHeight / 2f),
        size = Size(noseWidth, noseHeight),
        cornerRadius = CornerRadius(6f * scale, 4f * scale)
      )

      // Eyes & Eyebrows based on state
      val eyeY = centerY - 12f * scale
      val eyeSpacing = 20f * scale
      val eyeGlance = if (state == TedState.PROCESSING) glanceOffset * scale else 0f

      when (state) {
        TedState.ROASTING -> {
          // Left Eye: Cocky Open
          drawCircle(
            color = Color(0xFF241810),
            radius = 5f * scale,
            center = Offset(centerX - eyeSpacing, eyeY)
          )
          // Catchlight
          drawCircle(
            color = Color.White,
            radius = 1.8f * scale,
            center = Offset(centerX - eyeSpacing - 1.5f * scale, eyeY - 1.5f * scale)
          )
          // Left Eyebrow (Arching high)
          val leftBrow = Path().apply {
            moveTo(centerX - eyeSpacing - 8f * scale, eyeY - 16f * scale)
            quadraticTo(
              centerX - eyeSpacing, eyeY - 22f * scale,
              centerX - eyeSpacing + 8f * scale, eyeY - 13f * scale
            )
          }
          drawPath(leftBrow, Color(0xFF241810), style = Stroke(width = 3.5f * scale, cap = StrokeCap.Round))

          // Right Eye: Wink / Closed smirk line
          val rightWink = Path().apply {
            moveTo(centerX + eyeSpacing - 7f * scale, eyeY + 1f * scale)
            quadraticTo(
              centerX + eyeSpacing, eyeY - 3f * scale,
              centerX + eyeSpacing + 7f * scale, eyeY + 1f * scale
            )
          }
          drawPath(rightWink, Color(0xFF241810), style = Stroke(width = 3.5f * scale, cap = StrokeCap.Round))
          // Right Eyebrow: Skeptical flat
          drawLine(
            color = Color(0xFF241810),
            start = Offset(centerX + eyeSpacing - 8f * scale, eyeY - 12f * scale),
            end = Offset(centerX + eyeSpacing + 8f * scale, eyeY - 10f * scale),
            strokeWidth = 3.5f * scale,
            cap = StrokeCap.Round
          )
        }
        TedState.LISTENING -> {
          // Attentive wide eyes
          drawCircle(color = Color(0xFF241810), radius = 6f * scale, center = Offset(centerX - eyeSpacing, eyeY))
          drawCircle(color = Color(0xFF241810), radius = 6f * scale, center = Offset(centerX + eyeSpacing, eyeY))
          // Catchlights
          drawCircle(color = Color.White, radius = 2.2f * scale, center = Offset(centerX - eyeSpacing - 1.5f * scale, eyeY - 2f * scale))
          drawCircle(color = Color.White, radius = 2.2f * scale, center = Offset(centerX + eyeSpacing - 1.5f * scale, eyeY - 2f * scale))
          // Raised interested brows
          val leftBrow = Path().apply {
            moveTo(centerX - eyeSpacing - 8f * scale, eyeY - 15f * scale)
            quadraticTo(centerX - eyeSpacing, eyeY - 20f * scale, centerX - eyeSpacing + 8f * scale, eyeY - 14f * scale)
          }
          val rightBrow = Path().apply {
            moveTo(centerX + eyeSpacing - 8f * scale, eyeY - 14f * scale)
            quadraticTo(centerX + eyeSpacing, eyeY - 20f * scale, centerX + eyeSpacing + 8f * scale, eyeY - 15f * scale)
          }
          drawPath(leftBrow, Color(0xFF241810), style = Stroke(width = 3.2f * scale, cap = StrokeCap.Round))
          drawPath(rightBrow, Color(0xFF241810), style = Stroke(width = 3.2f * scale, cap = StrokeCap.Round))
        }
        else -> {
          // IDLE / PROCESSING / SPEAKING
          // Left Eye (With glance offset during processing)
          drawCircle(
            color = Color(0xFF241810),
            radius = 5.2f * scale,
            center = Offset(centerX - eyeSpacing + eyeGlance, eyeY)
          )
          drawCircle(
            color = Color.White,
            radius = 1.8f * scale,
            center = Offset(centerX - eyeSpacing + eyeGlance - 1.5f * scale, eyeY - 1.5f * scale)
          )

          // Right Eye
          drawCircle(
            color = Color(0xFF241810),
            radius = 5.2f * scale,
            center = Offset(centerX + eyeSpacing + eyeGlance, eyeY)
          )
          drawCircle(
            color = Color.White,
            radius = 1.8f * scale,
            center = Offset(centerX + eyeSpacing + eyeGlance - 1.5f * scale, eyeY - 1.5f * scale)
          )

          // Eyebrows: Classic Ted cocky smirk arch
          val leftBrow = Path().apply {
            moveTo(centerX - eyeSpacing - 8f * scale, eyeY - 14f * scale)
            quadraticTo(
              centerX - eyeSpacing, eyeY - 19f * scale,
              centerX - eyeSpacing + 8f * scale, eyeY - 12f * scale
            )
          }
          drawPath(leftBrow, Color(0xFF241810), style = Stroke(width = 3.2f * scale, cap = StrokeCap.Round))

          // Right Brow: Slightly flatter, cynical look
          drawLine(
            color = Color(0xFF241810),
            start = Offset(centerX + eyeSpacing - 7f * scale, eyeY - 12f * scale),
            end = Offset(centerX + eyeSpacing + 8f * scale, eyeY - 10f * scale),
            strokeWidth = 3.2f * scale,
            cap = StrokeCap.Round
          )
        }
      }

      // Mouth: Animated when speaking, smirk when idle/roasting
      if (state == TedState.SPEAKING) {
        // Open animated talking mouth
        val mouthHeight = (12f * mouthOpen + 3f) * scale
        val mouthWidth = 18f * scale
        val mouthY = muzzleCenterY + 4f * scale
        drawRoundRect(
          color = Color(0xFF6B1818),
          topLeft = Offset(centerX - mouthWidth / 2f, mouthY),
          size = Size(mouthWidth, mouthHeight),
          cornerRadius = CornerRadius(6f * scale, 6f * scale)
        )
        // Little white tooth or pink tongue
        drawRoundRect(
          color = Color(0xFFE57373),
          topLeft = Offset(centerX - (mouthWidth * 0.6f) / 2f, mouthY + mouthHeight * 0.55f),
          size = Size(mouthWidth * 0.6f, mouthHeight * 0.45f),
          cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )
      } else {
        // Closed sarcastic smirk mouth
        val philtrum = Path().apply {
          moveTo(centerX, noseCenterY + noseHeight / 2f)
          lineTo(centerX, muzzleCenterY + 2f * scale)
        }
        drawPath(philtrum, Color(0xFF241810), style = Stroke(width = 2.6f * scale, cap = StrokeCap.Round))

        val smirk = Path().apply {
          moveTo(centerX - 6f * scale, muzzleCenterY + 4f * scale)
          quadraticTo(
            centerX + 3f * scale, muzzleCenterY + 7f * scale,
            centerX + 14f * scale, muzzleCenterY + 2f * scale
          )
        }
        drawPath(smirk, Color(0xFF241810), style = Stroke(width = 3f * scale, cap = StrokeCap.Round))
      }

      // Ted's sharp red tie / collar at bottom
      val tieTopY = centerY + 42f * scale
      val tiePath = Path().apply {
        moveTo(centerX - 8f * scale, tieTopY)
        lineTo(centerX + 8f * scale, tieTopY)
        lineTo(centerX + 6f * scale, tieTopY + 18f * scale)
        lineTo(centerX, tieTopY + 24f * scale)
        lineTo(centerX - 6f * scale, tieTopY + 18f * scale)
        close()
      }
      drawPath(tiePath, Color(0xFFD32F2F), style = Fill)
      // Tie knot
      drawRoundRect(
        color = Color(0xFFB71C1C),
        topLeft = Offset(centerX - 7f * scale, tieTopY - 2f * scale),
        size = Size(14f * scale, 7f * scale),
        cornerRadius = CornerRadius(3f * scale, 3f * scale)
      )
    }
  }
}
