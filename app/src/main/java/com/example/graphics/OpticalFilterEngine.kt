package com.example.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.ColorTint
import com.example.model.FilterMode
import com.example.model.OpticsConfig
import com.example.model.PresetStudy
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object OpticalFilterEngine {

  fun applyFilters(source: Bitmap, config: OpticsConfig): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val outputPixels = when (config.filterMode) {
      FilterMode.RAW -> processTone(pixels, width, height, config)
      FilterMode.SOBEL_EDGE -> processSobel(pixels, width, height, config)
      FilterMode.STENCIL_THRESHOLD -> processStencil(pixels, width, height, config)
      FilterMode.LUMA_INVERT -> processInvert(pixels, width, height, config)
      FilterMode.POSTERIZE -> processPosterize(pixels, width, height, config)
      FilterMode.MONO_CHROMATIC -> processMonochrome(pixels, width, height, config)
    }

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(outputPixels, 0, width, 0, 0, width, height)
    return result
  }

  private fun processTone(pixels: IntArray, width: Int, height: Int, config: OpticsConfig): IntArray {
    val output = IntArray(pixels.size)
    val contrastFactor = (259.0f * (config.contrast + 255.0f)) / (255.0f * (259.0f - config.contrast))
    val brightnessShift = config.brightness
    val satRatio = config.saturation / 100.0f

    for (i in pixels.indices) {
      val c = pixels[i]
      val a = (c ushr 24) and 0xFF
      var r = (c ushr 16) and 0xFF
      var g = (c ushr 8) and 0xFF
      var b = c and 0xFF

      r = clampPixel((contrastFactor * (r - 128) + 128 + brightnessShift).toInt())
      g = clampPixel((contrastFactor * (g - 128) + 128 + brightnessShift).toInt())
      b = clampPixel((contrastFactor * (b - 128) + 128 + brightnessShift).toInt())

      val lum = 0.299f * r + 0.587f * g + 0.114f * b
      r = clampPixel((lum + satRatio * (r - lum)).toInt())
      g = clampPixel((lum + satRatio * (g - lum)).toInt())
      b = clampPixel((lum + satRatio * (b - lum)).toInt())

      output[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    return output
  }

  private fun processSobel(pixels: IntArray, width: Int, height: Int, config: OpticsConfig): IntArray {
    val output = IntArray(width * height)
    val gray = IntArray(width * height)

    for (i in pixels.indices) {
      val c = pixels[i]
      val r = (c ushr 16) and 0xFF
      val g = (c ushr 8) and 0xFF
      val b = c and 0xFF
      gray[i] = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
    }

    val threshold = config.edgeThreshold
    val tintR = when (config.tint) {
      ColorTint.CYAN -> 0x00
      ColorTint.VERMILION -> 0xFF
      ColorTint.EMERALD -> 0x10
      ColorTint.NONE -> 0xEC
    }
    val tintG = when (config.tint) {
      ColorTint.CYAN -> 0xE5
      ColorTint.VERMILION -> 0x4F
      ColorTint.EMERALD -> 0xB9
      ColorTint.NONE -> 0xEE
    }
    val tintB = when (config.tint) {
      ColorTint.CYAN -> 0xFF
      ColorTint.VERMILION -> 0x00
      ColorTint.EMERALD -> 0x81
      ColorTint.NONE -> 0xF2
    }

    for (y in 1 until height - 1) {
      val rowAbove = (y - 1) * width
      val rowCurrent = y * width
      val rowBelow = (y + 1) * width

      for (x in 1 until width - 1) {
        val gx = (-gray[rowAbove + x - 1] + gray[rowAbove + x + 1]
          - 2 * gray[rowCurrent + x - 1] + 2 * gray[rowCurrent + x + 1]
          - gray[rowBelow + x - 1] + gray[rowBelow + x + 1])

        val gy = (-gray[rowAbove + x - 1] - 2 * gray[rowAbove + x] - gray[rowAbove + x + 1]
          + gray[rowBelow + x - 1] + 2 * gray[rowBelow + x] + gray[rowBelow + x + 1])

        val mag = hypot(gx.toDouble(), gy.toDouble()).toInt()
        val idx = rowCurrent + x

        if (mag >= threshold) {
          val alpha = clampPixel(min(255, (mag * 255) / max(1, threshold + 60)))
          output[idx] = (alpha shl 24) or (tintR shl 16) or (tintG shl 8) or tintB
        } else {
          output[idx] = 0x00000000
        }
      }
    }
    return output
  }

  private fun processStencil(pixels: IntArray, width: Int, height: Int, config: OpticsConfig): IntArray {
    val output = IntArray(pixels.size)
    val threshold = config.edgeThreshold

    for (i in pixels.indices) {
      val c = pixels[i]
      val r = (c ushr 16) and 0xFF
      val g = (c ushr 8) and 0xFF
      val b = c and 0xFF
      val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()

      output[i] = if (lum < threshold) {
        0xFF090A0C.toInt()
      } else {
        0xFFECEEF2.toInt()
      }
    }
    return output
  }

  private fun processInvert(pixels: IntArray, width: Int, height: Int, config: OpticsConfig): IntArray {
    val output = IntArray(pixels.size)
    for (i in pixels.indices) {
      val c = pixels[i]
      val a = (c ushr 24) and 0xFF
      val r = 255 - ((c ushr 16) and 0xFF)
      val g = 255 - ((c ushr 8) and 0xFF)
      val b = 255 - (c and 0xFF)
      output[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    return output
  }

  private fun processPosterize(pixels: IntArray, width: Int, height: Int, config: OpticsConfig): IntArray {
    val output = IntArray(pixels.size)
    for (i in pixels.indices) {
      val c = pixels[i]
      val a = (c ushr 24) and 0xFF
      val r = ((c ushr 16) and 0xFF) / 64 * 85
      val g = ((c ushr 8) and 0xFF) / 64 * 85
      val b = (c and 0xFF) / 64 * 85
      output[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    return output
  }

  private fun processMonochrome(pixels: IntArray, width: Int, height: Int, config: OpticsConfig): IntArray {
    val output = IntArray(pixels.size)
    val contrastFactor = (259.0f * (config.contrast + 255.0f)) / (255.0f * (259.0f - config.contrast))

    for (i in pixels.indices) {
      val c = pixels[i]
      val a = (c ushr 24) and 0xFF
      val r = (c ushr 16) and 0xFF
      val g = (c ushr 8) and 0xFF
      val b = c and 0xFF
      var lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
      lum = clampPixel((contrastFactor * (lum - 128) + 128 + config.brightness).toInt())
      output[i] = (a shl 24) or (lum shl 16) or (lum shl 8) or lum
    }
    return output
  }

  private fun clampPixel(v: Int): Int = max(0, min(255, v))

  fun createPresetBitmap(study: PresetStudy, width: Int = 1080, height: Int = 1080): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(0xFF090A0C.toInt())

    val strokePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 3f
      color = 0xFFECEEF2.toInt()
    }

    val fillPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.FILL
      color = 0xFFECEEF2.toInt()
    }

    val guidePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
      color = 0xFF23262E.toInt()
    }

    val accentPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 2.5f
      color = 0xFFFF4F00.toInt()
    }

    when (study) {
      PresetStudy.SKULL_STUDY -> drawSkullStudy(canvas, width, height, strokePaint, fillPaint, guidePaint, accentPaint)
      PresetStudy.ARCHITECTURE -> drawArchitectureStudy(canvas, width, height, strokePaint, guidePaint, accentPaint)
      PresetStudy.BOTANICAL -> drawBotanicalStudy(canvas, width, height, strokePaint, guidePaint, accentPaint)
      PresetStudy.PORTRAIT_PROPORTIONS -> drawPortraitStudy(canvas, width, height, strokePaint, guidePaint, accentPaint)
    }

    return bmp
  }

  private fun drawSkullStudy(
    canvas: Canvas,
    w: Int,
    h: Int,
    stroke: Paint,
    fill: Paint,
    guide: Paint,
    accent: Paint
  ) {
    val cx = w * 0.5f
    val cy = h * 0.48f

    canvas.drawCircle(cx, cy - 60f, 260f, guide)
    canvas.drawLine(cx, cy - 360f, cx, cy + 380f, guide)
    canvas.drawLine(cx - 320f, cy, cx + 320f, cy, guide)

    val cranium = Path().apply {
      moveTo(cx - 190f, cy + 60f)
      cubicTo(cx - 260f, cy - 80f, cx - 240f, cy - 280f, cx, cy - 320f)
      cubicTo(cx + 240f, cy - 320f, cx + 260f, cy - 80f, cx + 190f, cy + 60f)
      lineTo(cx + 170f, cy + 120f)
      lineTo(cx + 120f, cy + 160f)
      lineTo(cx + 90f, cy + 290f)
      lineTo(cx - 90f, cy + 290f)
      lineTo(cx - 120f, cy + 160f)
      lineTo(cx - 170f, cy + 120f)
      close()
    }
    canvas.drawPath(cranium, stroke)

    val leftSocket = RectF(cx - 145f, cy - 20f, cx - 35f, cy + 90f)
    val rightSocket = RectF(cx + 35f, cy - 20f, cx + 145f, cy + 90f)
    canvas.drawRoundRect(leftSocket, 28f, 28f, stroke)
    canvas.drawRoundRect(rightSocket, 28f, 28f, stroke)

    val nasal = Path().apply {
      moveTo(cx, cy + 60f)
      lineTo(cx + 24f, cy + 140f)
      lineTo(cx, cy + 160f)
      lineTo(cx - 24f, cy + 140f)
      close()
    }
    canvas.drawPath(nasal, stroke)

    for (t in -4..4) {
      val tx = cx + t * 20f
      canvas.drawLine(tx, cy + 205f, tx, cy + 235f, stroke)
      canvas.drawLine(tx, cy + 245f, tx, cy + 275f, stroke)
    }
    canvas.drawLine(cx - 95f, cy + 240f, cx + 95f, cy + 240f, accent)

    val suture = Path().apply {
      moveTo(cx, cy - 320f)
      for (step in 1..8) {
        val sy = cy - 320f + step * 25f
        val sx = cx + (if (step % 2 == 0) 14f else -14f)
        lineTo(sx, sy)
      }
    }
    canvas.drawPath(suture, guide)
  }

  private fun drawArchitectureStudy(
    canvas: Canvas,
    w: Int,
    h: Int,
    stroke: Paint,
    guide: Paint,
    accent: Paint
  ) {
    val horizonY = h * 0.45f
    val vpLeftX = w * 0.08f
    val vpRightX = w * 0.92f

    canvas.drawLine(0f, horizonY, w.toFloat(), horizonY, guide)
    canvas.drawCircle(vpLeftX, horizonY, 8f, accent)
    canvas.drawCircle(vpRightX, horizonY, 8f, accent)

    val cornerX = w * 0.48f
    val cornerTop = h * 0.22f
    val cornerBottom = h * 0.78f

    canvas.drawLine(cornerX, cornerTop, cornerX, cornerBottom, stroke)

    canvas.drawLine(cornerX, cornerTop, vpLeftX, horizonY, guide)
    canvas.drawLine(cornerX, cornerBottom, vpLeftX, horizonY, guide)
    canvas.drawLine(cornerX, cornerTop, vpRightX, horizonY, guide)
    canvas.drawLine(cornerX, cornerBottom, vpRightX, horizonY, guide)

    val leftWallX = w * 0.25f
    val rightWallX = w * 0.76f

    val leftTopY = horizonY + (cornerTop - horizonY) * (leftWallX - vpLeftX) / (cornerX - vpLeftX)
    val leftBottomY = horizonY + (cornerBottom - horizonY) * (leftWallX - vpLeftX) / (cornerX - vpLeftX)
    canvas.drawLine(leftWallX, leftTopY, leftWallX, leftBottomY, stroke)
    canvas.drawLine(leftWallX, leftTopY, cornerX, cornerTop, stroke)
    canvas.drawLine(leftWallX, leftBottomY, cornerX, cornerBottom, stroke)

    val rightTopY = horizonY + (cornerTop - horizonY) * (vpRightX - rightWallX) / (vpRightX - cornerX)
    val rightBottomY = horizonY + (cornerBottom - horizonY) * (vpRightX - rightWallX) / (vpRightX - cornerX)
    canvas.drawLine(rightWallX, rightTopY, rightWallX, rightBottomY, stroke)
    canvas.drawLine(cornerX, cornerTop, rightWallX, rightTopY, stroke)
    canvas.drawLine(cornerX, cornerBottom, rightWallX, rightBottomY, stroke)

    for (i in 1..4) {
      val t = i / 5.0f
      val mullionX = cornerX + (rightWallX - cornerX) * t
      val mTop = cornerTop + (rightTopY - cornerTop) * t
      val mBottom = cornerBottom + (rightBottomY - cornerBottom) * t
      canvas.drawLine(mullionX, mTop, mullionX, mBottom, stroke)
    }

    val canopy = Path().apply {
      moveTo(cornerX - 60f, cornerTop - 40f)
      lineTo(cornerX + 160f, cornerTop - 70f)
      lineTo(cornerX + 220f, cornerTop - 40f)
      lineTo(cornerX, cornerTop - 10f)
      close()
    }
    canvas.drawPath(canopy, accent)
  }

  private fun drawBotanicalStudy(
    canvas: Canvas,
    w: Int,
    h: Int,
    stroke: Paint,
    guide: Paint,
    accent: Paint
  ) {
    val cx = w * 0.5f
    val cy = h * 0.5f

    val stem = Path().apply {
      moveTo(cx, h * 0.88f)
      cubicTo(cx - 30f, cy + 180f, cx + 20f, cy - 100f, cx, h * 0.14f)
    }
    canvas.drawPath(stem, accent)

    val blade = Path().apply {
      moveTo(cx, h * 0.14f)
      cubicTo(cx + 260f, cy - 140f, cx + 320f, cy + 160f, cx, h * 0.78f)
      cubicTo(cx - 320f, cy + 160f, cx - 260f, cy - 140f, cx, h * 0.14f)
    }
    canvas.drawPath(blade, stroke)

    for (step in 1..7) {
      val t = step / 8.0f
      val vy = (h * 0.78f) * (1 - t) + (h * 0.14f) * t
      val span = 190f * sin(t * Math.PI).toFloat()

      canvas.drawLine(cx, vy, cx + span, vy - 45f, stroke)
      canvas.drawLine(cx, vy, cx - span, vy - 45f, stroke)

      canvas.drawLine(cx + span, vy - 45f, cx + span + 28f, vy - 65f, guide)
      canvas.drawLine(cx - span, vy - 45f, cx - span - 28f, vy - 65f, guide)
    }
  }

  private fun drawPortraitStudy(
    canvas: Canvas,
    w: Int,
    h: Int,
    stroke: Paint,
    guide: Paint,
    accent: Paint
  ) {
    val cx = w * 0.5f
    val cy = h * 0.48f

    canvas.drawCircle(cx, cy - 40f, 240f, guide)
    canvas.drawLine(cx, cy - 360f, cx, cy + 380f, guide)

    val browY = cy - 40f
    val noseY = cy + 80f
    val chinY = cy + 240f
    val hairY = cy - 200f
    val eyeY = cy + 10f
    val mouthY = cy + 155f

    canvas.drawLine(cx - 240f, browY, cx + 240f, browY, guide)
    canvas.drawLine(cx - 240f, noseY, cx + 240f, noseY, guide)
    canvas.drawLine(cx - 200f, chinY, cx + 200f, chinY, accent)
    canvas.drawLine(cx - 220f, hairY, cx + 220f, hairY, guide)
    canvas.drawLine(cx - 240f, eyeY, cx + 240f, eyeY, stroke)
    canvas.drawLine(cx - 160f, mouthY, cx + 160f, mouthY, stroke)

    val jaw = Path().apply {
      moveTo(cx - 220f, browY)
      lineTo(cx - 200f, noseY)
      lineTo(cx - 140f, cy + 180f)
      lineTo(cx - 60f, chinY)
      lineTo(cx + 60f, chinY)
      lineTo(cx + 140f, cy + 180f)
      lineTo(cx + 200f, noseY)
      lineTo(cx + 220f, browY)
    }
    canvas.drawPath(jaw, stroke)

    canvas.drawCircle(cx - 95f, eyeY, 18f, stroke)
    canvas.drawCircle(cx + 95f, eyeY, 18f, stroke)

    val nosePath = Path().apply {
      moveTo(cx, eyeY)
      lineTo(cx - 18f, noseY)
      lineTo(cx + 18f, noseY)
      close()
    }
    canvas.drawPath(nosePath, stroke)
  }

  fun createCalibrationViewfinder(width: Int = 1080, height: Int = 1080): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(0xFF090A0C.toInt())

    val guidePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
      color = 0xFF23262E.toInt()
    }

    val orangePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 2f
      color = 0xFFFF4F00.toInt()
    }

    val cyanPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
      color = 0xFF00E5FF.toInt()
    }

    val cx = width * 0.5f
    val cy = height * 0.5f

    for (r in listOf(120f, 240f, 380f)) {
      canvas.drawCircle(cx, cy, r, guidePaint)
    }

    canvas.drawLine(cx - 420f, cy, cx + 420f, cy, guidePaint)
    canvas.drawLine(cx, cy - 420f, cx, cy + 420f, guidePaint)

    val frameW = width * 0.78f
    val frameH = height * 0.78f
    val rect = RectF(cx - frameW * 0.5f, cy - frameH * 0.5f, cx + frameW * 0.5f, cy + frameH * 0.5f)
    canvas.drawRect(rect, orangePaint)

    val tickLen = 22f
    canvas.drawLine(rect.left, rect.top, rect.left + tickLen, rect.top, cyanPaint)
    canvas.drawLine(rect.left, rect.top, rect.left, rect.top + tickLen, cyanPaint)
    canvas.drawLine(rect.right, rect.top, rect.right - tickLen, rect.top, cyanPaint)
    canvas.drawLine(rect.right, rect.top, rect.right, rect.top + tickLen, cyanPaint)
    canvas.drawLine(rect.left, rect.bottom, rect.left + tickLen, rect.bottom, cyanPaint)
    canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - tickLen, cyanPaint)
    canvas.drawLine(rect.right, rect.bottom, rect.right - tickLen, rect.bottom, cyanPaint)
    canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - tickLen, cyanPaint)

    return bmp
  }
}
