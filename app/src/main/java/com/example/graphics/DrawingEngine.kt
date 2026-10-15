package com.example.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.BlendModeType
import com.example.model.BrushConfig
import com.example.model.LayerItem
import com.example.model.PointRecord
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.model.VectorShapeSubtype
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class DrawingEngine(val width: Int = 1080, val height: Int = 1080) {

  private val layerBitmaps = mutableMapOf<Int, Bitmap>()
  private val layerCanvases = mutableMapOf<Int, Canvas>()

  private val undoStack = ArrayDeque<Snapshot>()
  private val redoStack = ArrayDeque<Snapshot>()
  private val maxUndoSteps = 25

  data class Snapshot(
    val layerId: Int,
    val bitmap: Bitmap
  )

  init {
    for (id in 1..4) {
      val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      layerBitmaps[id] = bmp
      layerCanvases[id] = Canvas(bmp)
    }
  }

  fun getLayerBitmap(layerId: Int): Bitmap? = layerBitmaps[layerId]

  fun saveSnapshot(activeLayerId: Int) {
    val current = layerBitmaps[activeLayerId] ?: return
    val copy = current.copy(Bitmap.Config.ARGB_8888, true)
    if (undoStack.size >= maxUndoSteps) {
      undoStack.removeFirst()
    }
    undoStack.addLast(Snapshot(activeLayerId, copy))
    redoStack.clear()
  }

  fun canUndo(): Boolean = undoStack.isNotEmpty()
  fun canRedo(): Boolean = redoStack.isNotEmpty()

  fun undo(activeLayerId: Int): Boolean {
    if (undoStack.isEmpty()) return false
    val current = layerBitmaps[activeLayerId] ?: return false
    val currentCopy = current.copy(Bitmap.Config.ARGB_8888, true)
    redoStack.addLast(Snapshot(activeLayerId, currentCopy))

    val snapshot = undoStack.removeLast()
    val targetBmp = layerBitmaps[snapshot.layerId] ?: return false
    val canvas = layerCanvases[snapshot.layerId] ?: return false
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    canvas.drawBitmap(snapshot.bitmap, 0f, 0f, null)
    return true
  }

  fun redo(activeLayerId: Int): Boolean {
    if (redoStack.isEmpty()) return false
    val current = layerBitmaps[activeLayerId] ?: return false
    val currentCopy = current.copy(Bitmap.Config.ARGB_8888, true)
    undoStack.addLast(Snapshot(activeLayerId, currentCopy))

    val snapshot = redoStack.removeLast()
    val targetBmp = layerBitmaps[snapshot.layerId] ?: return false
    val canvas = layerCanvases[snapshot.layerId] ?: return false
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    canvas.drawBitmap(snapshot.bitmap, 0f, 0f, null)
    return true
  }

  fun clearLayer(layerId: Int) {
    saveSnapshot(layerId)
    val canvas = layerCanvases[layerId] ?: return
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
  }

  fun duplicateLayer(sourceId: Int, destId: Int) {
    val src = layerBitmaps[sourceId] ?: return
    saveSnapshot(destId)
    val destCanvas = layerCanvases[destId] ?: return
    destCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    destCanvas.drawBitmap(src, 0f, 0f, null)
  }

  fun mergeDown(upperId: Int, lowerId: Int) {
    val upper = layerBitmaps[upperId] ?: return
    val lowerCanvas = layerCanvases[lowerId] ?: return
    saveSnapshot(lowerId)
    lowerCanvas.drawBitmap(upper, 0f, 0f, null)
    clearLayer(upperId)
  }

  fun samplePixelColor(x: Float, y: Float, activeLayerId: Int, referenceBitmap: Bitmap?): Long {
    val ix = x.toInt().coerceIn(0, width - 1)
    val iy = y.toInt().coerceIn(0, height - 1)

    val activeBmp = layerBitmaps[activeLayerId]
    if (activeBmp != null) {
      val pixel = activeBmp.getPixel(ix, iy)
      if ((pixel ushr 24) and 0xFF > 15) {
        return pixel.toLong() and 0xFFFFFFFFL
      }
    }

    if (referenceBitmap != null && !referenceBitmap.isRecycled) {
      val rx = (x * referenceBitmap.width / width).toInt().coerceIn(0, referenceBitmap.width - 1)
      val ry = (y * referenceBitmap.height / height).toInt().coerceIn(0, referenceBitmap.height - 1)
      val pixel = referenceBitmap.getPixel(rx, ry)
      return pixel.toLong() and 0xFFFFFFFFL
    }

    return 0xFFECEEF2L
  }

  fun renderStrokeSegment(
    points: List<PointRecord>,
    activeLayerId: Int,
    tool: ToolType,
    brush: BrushConfig,
    symmetry: SymmetryMode
  ) {
    if (points.size < 2) return
    val canvas = layerCanvases[activeLayerId] ?: return
    val p0 = points[points.size - 2]
    val p1 = points[points.size - 1]

    val symmetryPoints = generateSymmetryPoints(p0, p1, symmetry, width * 0.5f, height * 0.5f)

    for ((start, end) in symmetryPoints) {
      when (tool) {
        ToolType.INK_PEN -> drawSplineSegment(canvas, start, end, brush)
        ToolType.GRAIN_BRUSH -> drawGrainSegment(canvas, start, end, brush)
        ToolType.CHISEL_MARKER -> drawChiselSegment(canvas, start, end, brush)
        ToolType.ERASER -> drawEraserSegment(canvas, start, end, brush)
        else -> {}
      }
    }
  }

  private fun drawSplineSegment(canvas: Canvas, p0: PointRecord, p1: PointRecord, brush: BrushConfig) {
    val paint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      color = (brush.color.toInt())
      alpha = (brush.opacity * 255).toInt().coerceIn(0, 255)
      strokeWidth = max(1f, brush.size * p1.pressure)
    }

    val midX = (p0.x + p1.x) * 0.5f
    val midY = (p0.y + p1.y) * 0.5f
    val path = Path().apply {
      moveTo(p0.x, p0.y)
      quadTo(p0.x, p0.y, midX, midY)
      lineTo(p1.x, p1.y)
    }
    canvas.drawPath(path, paint)
  }

  private fun drawGrainSegment(canvas: Canvas, p0: PointRecord, p1: PointRecord, brush: BrushConfig) {
    val paint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.FILL
      color = brush.color.toInt()
    }

    val dist = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
    val steps = max(1, (dist / (brush.size * 0.25f)).toInt())
    val radius = brush.size * 0.5f

    for (s in 0..steps) {
      val t = s.toFloat() / steps
      val cx = p0.x + (p1.x - p0.x) * t
      val cy = p0.y + (p1.y - p0.y) * t

      val bristleCount = max(6, (brush.size * 0.8f).toInt())
      for (b in 0 until bristleCount) {
        val angle = Random.nextFloat() * 6.283185f
        val r = radius * Random.nextFloat()
        val dotAlpha = (brush.opacity * 255 * (1.0f - r / radius) * 0.45f).toInt().coerceIn(1, 255)
        paint.alpha = dotAlpha
        canvas.drawCircle(cx + cos(angle) * r, cy + sin(angle) * r, 1.4f, paint)
      }
    }
  }

  private fun drawChiselSegment(canvas: Canvas, p0: PointRecord, p1: PointRecord, brush: BrushConfig) {
    val paint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.BUTT
      color = brush.color.toInt()
      alpha = (brush.opacity * 130).toInt().coerceIn(1, 255)
      strokeWidth = max(2f, brush.size)
    }

    val nibAngle = 0.785398f
    val nibLen = brush.size * 0.5f
    val nx = cos(nibAngle) * nibLen
    val ny = sin(nibAngle) * nibLen

    val path = Path().apply {
      moveTo(p0.x - nx, p0.y - ny)
      lineTo(p0.x + nx, p0.y + ny)
      lineTo(p1.x + nx, p1.y + ny)
      lineTo(p1.x - nx, p1.y - ny)
      close()
    }

    val fillPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.FILL
      color = brush.color.toInt()
      alpha = (brush.opacity * 110).toInt().coerceIn(1, 255)
    }
    canvas.drawPath(path, fillPaint)
    canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
  }

  private fun drawEraserSegment(canvas: Canvas, p0: PointRecord, p1: PointRecord, brush: BrushConfig) {
    val paint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
      xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
      strokeWidth = max(2f, brush.size * 1.5f)
    }
    canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
  }

  fun drawVectorShape(
    start: PointRecord,
    end: PointRecord,
    shape: VectorShapeSubtype,
    activeLayerId: Int,
    brush: BrushConfig,
    symmetry: SymmetryMode
  ) {
    saveSnapshot(activeLayerId)
    val canvas = layerCanvases[activeLayerId] ?: return
    val pairs = generateSymmetryPoints(start, end, symmetry, width * 0.5f, height * 0.5f)

    val paint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.MITER
      color = brush.color.toInt()
      alpha = (brush.opacity * 255).toInt().coerceIn(0, 255)
      strokeWidth = max(1f, brush.size)
    }

    for ((p0, p1) in pairs) {
      when (shape) {
        VectorShapeSubtype.LINE -> {
          canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
        }
        VectorShapeSubtype.RECTANGLE -> {
          val left = min(p0.x, p1.x)
          val top = min(p0.y, p1.y)
          val right = max(p0.x, p1.x)
          val bottom = max(p0.y, p1.y)
          canvas.drawRect(left, top, right, bottom, paint)
        }
        VectorShapeSubtype.CIRCLE -> {
          val cx = (p0.x + p1.x) * 0.5f
          val cy = (p0.y + p1.y) * 0.5f
          val r = hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat() * 0.5f
          canvas.drawCircle(cx, cy, r, paint)
        }
      }
    }
  }

  private fun generateSymmetryPoints(
    p0: PointRecord,
    p1: PointRecord,
    mode: SymmetryMode,
    cx: Float,
    cy: Float
  ): List<Pair<PointRecord, PointRecord>> {
    val results = mutableListOf<Pair<PointRecord, PointRecord>>()
    results.add(Pair(p0, p1))

    when (mode) {
      SymmetryMode.OFF -> {}
      SymmetryMode.VERTICAL -> {
        results.add(
          Pair(
            PointRecord(2 * cx - p0.x, p0.y, p0.pressure),
            PointRecord(2 * cx - p1.x, p1.y, p1.pressure)
          )
        )
      }
      SymmetryMode.HORIZONTAL -> {
        results.add(
          Pair(
            PointRecord(p0.x, 2 * cy - p0.y, p0.pressure),
            PointRecord(p1.x, 2 * cy - p1.y, p1.pressure)
          )
        )
      }
      SymmetryMode.QUAD -> {
        results.add(
          Pair(
            PointRecord(2 * cx - p0.x, p0.y, p0.pressure),
            PointRecord(2 * cx - p1.x, p1.y, p1.pressure)
          )
        )
        results.add(
          Pair(
            PointRecord(p0.x, 2 * cy - p0.y, p0.pressure),
            PointRecord(p1.x, 2 * cy - p1.y, p1.pressure)
          )
        )
        results.add(
          Pair(
            PointRecord(2 * cx - p0.x, 2 * cy - p0.y, p0.pressure),
            PointRecord(2 * cx - p1.x, 2 * cy - p1.y, p1.pressure)
          )
        )
      }
      SymmetryMode.RADIAL_8 -> {
        for (i in 1..7) {
          val angle = (i * Math.PI / 4.0).toFloat()
          val cosA = cos(angle)
          val sinA = sin(angle)

          val dx0 = p0.x - cx
          val dy0 = p0.y - cy
          val rx0 = cx + dx0 * cosA - dy0 * sinA
          val ry0 = cy + dx0 * sinA + dy0 * cosA

          val dx1 = p1.x - cx
          val dy1 = p1.y - cy
          val rx1 = cx + dx1 * cosA - dy1 * sinA
          val ry1 = cy + dx1 * sinA + dy1 * cosA

          results.add(
            Pair(
              PointRecord(rx0, ry0, p0.pressure),
              PointRecord(rx1, ry1, p1.pressure)
            )
          )
        }
      }
    }

    return results
  }

  fun exportComposite(
    layers: List<LayerItem>,
    referenceBitmap: Bitmap?,
    referenceOpacity: Float,
    includeReference: Boolean,
    includeBackdrop: Boolean
  ): Bitmap {
    val composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(composite)

    if (includeBackdrop) {
      canvas.drawColor(0xFF090A0C.toInt())
    } else {
      canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    if (includeReference && referenceBitmap != null && !referenceBitmap.isRecycled) {
      val refPaint = Paint().apply {
        isAntiAlias = true
        alpha = (referenceOpacity * 255).toInt().coerceIn(0, 255)
      }
      val destRect = Rect(0, 0, width, height)
      canvas.drawBitmap(referenceBitmap, null, destRect, refPaint)
    }

    for (layer in layers) {
      if (!layer.isVisible) continue
      val bmp = layerBitmaps[layer.id] ?: continue
      val paint = Paint().apply {
        isAntiAlias = true
        alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
        when (layer.blendMode) {
          BlendModeType.MULTIPLY -> xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
          BlendModeType.SCREEN -> xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
          BlendModeType.OVERLAY -> xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
          BlendModeType.DIFFERENCE -> xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
          BlendModeType.NORMAL -> {}
        }
      }
      canvas.drawBitmap(bmp, 0f, 0f, paint)
    }

    return composite
  }
}
