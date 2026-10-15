package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.HudGridType
import com.example.model.SymmetryMode
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HudGridOverlay(
  gridType: HudGridType,
  gridOpacity: Float,
  symmetryMode: SymmetryMode,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val gridColor = Color(0xFF00E5FF).copy(alpha = gridOpacity.coerceIn(0.05f, 1.0f))
    val accentColor = Color(0xFFFF4F00).copy(alpha = gridOpacity.coerceIn(0.1f, 1.0f))

    when (gridType) {
      HudGridType.NONE -> {}
      HudGridType.RULE_OF_THIRDS -> drawRuleOfThirds(w, h, gridColor, accentColor)
      HudGridType.ARCHITECTURAL_16 -> drawArchitecturalGrid(w, h, gridColor)
      HudGridType.ISOMETRIC_30 -> drawIsometricGrid(w, h, gridColor)
      HudGridType.GOLDEN_SPIRAL -> drawGoldenSpiral(w, h, gridColor)
      HudGridType.TWO_POINT_PERSPECTIVE -> drawTwoPointPerspective(w, h, gridColor, accentColor)
    }

    drawSymmetryGuides(w, h, symmetryMode)
  }
}

private fun DrawScope.drawRuleOfThirds(w: Float, h: Float, color: Color, accent: Color) {
  val x1 = w / 3f
  val x2 = w * 2f / 3f
  val y1 = h / 3f
  val y2 = h * 2f / 3f

  val stroke = Stroke(width = 1.2f)
  drawLine(color, Offset(x1, 0f), Offset(x1, h), strokeWidth = 1.2f)
  drawLine(color, Offset(x2, 0f), Offset(x2, h), strokeWidth = 1.2f)
  drawLine(color, Offset(0f, y1), Offset(w, y1), strokeWidth = 1.2f)
  drawLine(color, Offset(0f, y2), Offset(w, y2), strokeWidth = 1.2f)

  val tickSize = 12f
  val points = listOf(Offset(x1, y1), Offset(x2, y1), Offset(x1, y2), Offset(x2, y2))
  for (pt in points) {
    drawLine(accent, Offset(pt.x - tickSize, pt.y), Offset(pt.x + tickSize, pt.y), strokeWidth = 2f)
    drawLine(accent, Offset(pt.x, pt.y - tickSize), Offset(pt.x, pt.y + tickSize), strokeWidth = 2f)
  }
}

private fun DrawScope.drawArchitecturalGrid(w: Float, h: Float, color: Color) {
  val cols = 16
  val rows = 16
  val stepX = w / cols
  val stepY = h / rows

  for (i in 1 until cols) {
    val x = i * stepX
    val isMajor = i % 4 == 0
    val alphaFactor = if (isMajor) 1.0f else 0.4f
    drawLine(color.copy(alpha = color.alpha * alphaFactor), Offset(x, 0f), Offset(x, h), strokeWidth = if (isMajor) 1.5f else 0.8f)
  }

  for (j in 1 until rows) {
    val y = j * stepY
    val isMajor = j % 4 == 0
    val alphaFactor = if (isMajor) 1.0f else 0.4f
    drawLine(color.copy(alpha = color.alpha * alphaFactor), Offset(0f, y), Offset(w, y), strokeWidth = if (isMajor) 1.5f else 0.8f)
  }
}

private fun DrawScope.drawIsometricGrid(w: Float, h: Float, color: Color) {
  val angleRad = 0.52359877f
  val slope = sin(angleRad) / cos(angleRad)
  val spacing = 48f

  var offset = -w
  while (offset < w + h) {
    drawLine(color, Offset(offset, 0f), Offset(offset + h / slope, h), strokeWidth = 0.8f)
    drawLine(color, Offset(offset, 0f), Offset(offset - h / slope, h), strokeWidth = 0.8f)
    offset += spacing
  }

  var y = 0f
  while (y <= h) {
    drawLine(color.copy(alpha = color.alpha * 0.4f), Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
    y += spacing * slope
  }
}

private fun DrawScope.drawGoldenSpiral(w: Float, h: Float, color: Color) {
  val path = Path()
  var left = 0f
  var top = 0f
  var right = w
  var bottom = h

  var cx = w * 0.5f
  var cy = h * 0.5f
  path.moveTo(left, bottom)

  val iterations = 8
  for (i in 0 until iterations) {
    when (i % 4) {
      0 -> {
        val span = (bottom - top) * 0.618033f
        drawRect(color.copy(alpha = color.alpha * 0.35f), Offset(left, top), Size(span, bottom - top), style = Stroke(0.8f))
        path.arcTo(Rect(left, top, left + span * 2, bottom + span), 90f, 90f, false)
        left += span
      }
      1 -> {
        val span = (right - left) * 0.618033f
        drawRect(color.copy(alpha = color.alpha * 0.35f), Offset(left, top), Size(right - left, span), style = Stroke(0.8f))
        path.arcTo(Rect(left - span, top, right, top + span * 2), 180f, 90f, false)
        top += span
      }
      2 -> {
        val span = (bottom - top) * 0.618033f
        drawRect(color.copy(alpha = color.alpha * 0.35f), Offset(right - span, top), Size(span, bottom - top), style = Stroke(0.8f))
        path.arcTo(Rect(right - span * 2, top - span, right, bottom), 270f, 90f, false)
        right -= span
      }
      3 -> {
        val span = (right - left) * 0.618033f
        drawRect(color.copy(alpha = color.alpha * 0.35f), Offset(left, bottom - span), Size(right - left, span), style = Stroke(0.8f))
        path.arcTo(Rect(left, bottom - span * 2, right + span, bottom), 0f, 90f, false)
        bottom -= span
      }
    }
  }

  drawPath(path, color, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawTwoPointPerspective(w: Float, h: Float, color: Color, accent: Color) {
  val horizonY = h * 0.45f
  val vp1 = Offset(w * 0.05f, horizonY)
  val vp2 = Offset(w * 0.95f, horizonY)

  drawLine(accent, Offset(0f, horizonY), Offset(w, horizonY), strokeWidth = 1.5f)
  drawCircle(accent, radius = 6f, center = vp1)
  drawCircle(accent, radius = 6f, center = vp2)

  val rayCount = 10
  for (i in 0..rayCount) {
    val bottomX = w * (i.toFloat() / rayCount)
    drawLine(color.copy(alpha = color.alpha * 0.5f), vp1, Offset(bottomX, h), strokeWidth = 0.8f)
    drawLine(color.copy(alpha = color.alpha * 0.5f), vp2, Offset(bottomX, h), strokeWidth = 0.8f)
    drawLine(color.copy(alpha = color.alpha * 0.3f), vp1, Offset(bottomX, 0f), strokeWidth = 0.8f)
    drawLine(color.copy(alpha = color.alpha * 0.3f), vp2, Offset(bottomX, 0f), strokeWidth = 0.8f)
  }
}

private fun DrawScope.drawSymmetryGuides(w: Float, h: Float, mode: SymmetryMode) {
  if (mode == SymmetryMode.OFF) return

  val cx = w * 0.5f
  val cy = h * 0.5f
  val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
  val stroke = Stroke(width = 1.5f, pathEffect = dash)
  val guideColor = Color(0xFFFF4F00).copy(alpha = 0.75f)

  when (mode) {
    SymmetryMode.OFF -> {}
    SymmetryMode.VERTICAL -> {
      drawLine(guideColor, Offset(cx, 0f), Offset(cx, h), strokeWidth = 1.5f, pathEffect = dash)
    }
    SymmetryMode.HORIZONTAL -> {
      drawLine(guideColor, Offset(0f, cy), Offset(w, cy), strokeWidth = 1.5f, pathEffect = dash)
    }
    SymmetryMode.QUAD -> {
      drawLine(guideColor, Offset(cx, 0f), Offset(cx, h), strokeWidth = 1.5f, pathEffect = dash)
      drawLine(guideColor, Offset(0f, cy), Offset(w, cy), strokeWidth = 1.5f, pathEffect = dash)
    }
    SymmetryMode.RADIAL_8 -> {
      drawLine(guideColor, Offset(cx, 0f), Offset(cx, h), strokeWidth = 1.5f, pathEffect = dash)
      drawLine(guideColor, Offset(0f, cy), Offset(w, cy), strokeWidth = 1.5f, pathEffect = dash)
      val d = kotlin.math.max(w, h)
      drawLine(guideColor, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeWidth = 1.2f, pathEffect = dash)
      drawLine(guideColor, Offset(cx - d, cy + d), Offset(cx + d, cy - d), strokeWidth = 1.2f, pathEffect = dash)
    }
  }

  drawCircle(guideColor, radius = 5f, center = Offset(cx, cy))
}
