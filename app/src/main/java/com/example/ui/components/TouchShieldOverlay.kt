package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.VoidBackdrop
import kotlinx.coroutines.launch

@Composable
fun TouchShieldOverlay(
  isLocked: Boolean,
  onUnlock: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!isLocked) return

  val progress = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 14.dp, end = 14.dp)
        .size(42.dp)
        .clip(CircleShape)
        .background(Color(0xD9090A0C))
        .border(1.dp, PrimaryAccent, CircleShape)
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = {
              scope.launch {
                progress.snapTo(0f)
                val animation = progress.animateTo(
                  targetValue = 1f,
                  animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
                if (progress.value >= 0.99f) {
                  onUnlock()
                  progress.snapTo(0f)
                }
              }
              val released = tryAwaitRelease()
              if (!released || progress.value < 0.99f) {
                scope.launch { progress.snapTo(0f) }
              }
            }
          )
        }
        .testTag("touch_shield_unlock_badge"),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeW = 3f
        val inset = strokeW * 0.5f
        drawArc(
          color = Color(0xFF23262E),
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = Size(size.width - strokeW, size.height - strokeW),
          style = Stroke(width = strokeW)
        )
        if (progress.value > 0.01f) {
          drawArc(
            color = Color(0xFFFF4F00),
            startAngle = -90f,
            sweepAngle = 360f * progress.value,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeW, size.height - strokeW),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
          )
        }
      }

      Text(
        text = if (progress.value > 0.05f) "${(progress.value * 100).toInt()}%" else "HOLD",
        fontSize = 7.sp,
        color = if (progress.value > 0.05f) PrimaryAccent else PrimaryText
      )
    }
  }
}
