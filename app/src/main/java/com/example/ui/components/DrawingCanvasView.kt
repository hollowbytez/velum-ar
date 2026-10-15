package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.graphics.DrawingEngine
import com.example.model.BrushConfig
import com.example.model.HudGridType
import com.example.model.LayerItem
import com.example.model.OpticsConfig
import com.example.model.PointRecord
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.model.VectorShapeSubtype
import com.example.model.WorkspaceMode
import com.example.ui.theme.VoidBackdrop
import com.example.ui.theme.WhitePure
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DrawingCanvasView(
  workspaceMode: WorkspaceMode,
  activeTool: ToolType,
  shapeSubtype: VectorShapeSubtype,
  symmetryMode: SymmetryMode,
  brushConfig: BrushConfig,
  opticsConfig: OpticsConfig,
  onOpticsConfigChange: (OpticsConfig) -> Unit,
  currentGhostOpacity: Float,
  layers: List<LayerItem>,
  activeLayerId: Int,
  drawingEngine: DrawingEngine,
  referenceBitmap: Bitmap?,
  fallbackBitmap: Bitmap?,
  isCameraLive: Boolean,
  previewView: PreviewView?,
  isTorchRingOn: Boolean,
  isMirrored: Boolean,
  hudGridType: HudGridType,
  hudGridOpacity: Float,
  onColorSampled: (Long) -> Unit,
  onFrameDrawn: () -> Unit,
  modifier: Modifier = Modifier
) {
  val strokePoints = remember { mutableStateListOf<PointRecord>() }
  var shapeStart by remember { mutableStateOf<PointRecord?>(null) }
  var shapeCurrent by remember { mutableStateOf<PointRecord?>(null) }
  var smoothedPoint by remember { mutableStateOf<PointRecord?>(null) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(VoidBackdrop)
  ) {
    if (workspaceMode != WorkspaceMode.SCREEN_STUDIO) {
      if (isCameraLive && previewView != null) {
        AndroidView(
          factory = { previewView },
          modifier = Modifier.fillMaxSize()
        )
      } else if (fallbackBitmap != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawImage(
            image = fallbackBitmap.asImageBitmap(),
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
          )
        }
      }
    }

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(activeTool, symmetryMode, brushConfig, opticsConfig) {
          when (activeTool) {
            ToolType.TRANSFORM -> {
              detectTransformGestures { centroid, pan, zoom, rotation ->
                val currentT = opticsConfig.transform
                val newScale = (currentT.scale * zoom).coerceIn(0.1f, 8.0f)
                val newRot = (currentT.rotation + rotation) % 360f
                val newPanX = currentT.panX + pan.x
                val newPanY = currentT.panY + pan.y
                onOpticsConfigChange(
                  opticsConfig.copy(
                    transform = currentT.copy(
                      panX = newPanX,
                      panY = newPanY,
                      scale = newScale,
                      rotation = newRot
                    )
                  )
                )
              }
            }
            ToolType.EYEDROPPER -> {
              detectTapGestures { offset ->
                val sampled = drawingEngine.samplePixelColor(
                  offset.x,
                  offset.y,
                  activeLayerId,
                  referenceBitmap
                )
                onColorSampled(sampled)
              }
            }
            ToolType.VECTOR_SHAPE -> {
              detectDragGestures(
                onDragStart = { offset ->
                  val pt = PointRecord(offset.x, offset.y)
                  shapeStart = pt
                  shapeCurrent = pt
                },
                onDrag = { change, _ ->
                  change.consume()
                  shapeCurrent = PointRecord(change.position.x, change.position.y)
                },
                onDragEnd = {
                  val s = shapeStart
                  val c = shapeCurrent
                  if (s != null && c != null) {
                    drawingEngine.drawVectorShape(
                      start = s,
                      end = c,
                      shape = shapeSubtype,
                      activeLayerId = activeLayerId,
                      brush = brushConfig,
                      symmetry = symmetryMode
                    )
                  }
                  shapeStart = null
                  shapeCurrent = null
                  onFrameDrawn()
                },
                onDragCancel = {
                  shapeStart = null
                  shapeCurrent = null
                }
              )
            }
            else -> {
              detectDragGestures(
                onDragStart = { offset ->
                  drawingEngine.saveSnapshot(activeLayerId)
                  strokePoints.clear()
                  val initial = PointRecord(offset.x, offset.y)
                  strokePoints.add(initial)
                  smoothedPoint = initial
                },
                onDrag = { change, _ ->
                  change.consume()
                  val rawX = change.position.x
                  val rawY = change.position.y
                  val alpha = 1.0f - opticsConfig.transform.tiltX.coerceIn(0f, 0.95f)
                  val prev = smoothedPoint ?: PointRecord(rawX, rawY)

                  val smoothFactor = brushConfig.stabilizer
                  val sx = prev.x * smoothFactor + rawX * (1.0f - smoothFactor)
                  val sy = prev.y * smoothFactor + rawY * (1.0f - smoothFactor)

                  val nextPoint = PointRecord(sx, sy)
                  smoothedPoint = nextPoint
                  strokePoints.add(nextPoint)

                  drawingEngine.renderStrokeSegment(
                    points = strokePoints,
                    activeLayerId = activeLayerId,
                    tool = activeTool,
                    brush = brushConfig,
                    symmetry = symmetryMode
                  )
                  onFrameDrawn()
                },
                onDragEnd = {
                  strokePoints.clear()
                  smoothedPoint = null
                },
                onDragCancel = {
                  strokePoints.clear()
                  smoothedPoint = null
                }
              )
            }
          }
        }
    ) {
      val w = size.width
      val h = size.height

      if (workspaceMode != WorkspaceMode.SCREEN_STUDIO && referenceBitmap != null && !referenceBitmap.isRecycled) {
        val t = opticsConfig.transform
        drawIntoCanvas { canvas ->
          val nativeCanvas = canvas.nativeCanvas
          nativeCanvas.save()

          if (isMirrored) {
            nativeCanvas.scale(-1f, 1f, w * 0.5f, h * 0.5f)
          }

          nativeCanvas.translate(t.panX, t.panY)
          nativeCanvas.rotate(t.rotation, w * 0.5f, h * 0.5f)
          nativeCanvas.scale(t.scale, t.scale, w * 0.5f, h * 0.5f)

          if (t.tiltX != 0f || t.tiltY != 0f) {
            val skewX = (t.tiltX / 45f) * 0.35f
            val skewY = (t.tiltY / 45f) * 0.35f
            nativeCanvas.skew(skewX, skewY)
          }

          val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            alpha = (currentGhostOpacity * 255).toInt().coerceIn(0, 255)
          }

          val srcRect = android.graphics.Rect(0, 0, referenceBitmap.width, referenceBitmap.height)
          val dstRect = android.graphics.Rect(0, 0, w.toInt(), h.toInt())
          nativeCanvas.drawBitmap(referenceBitmap, srcRect, dstRect, paint)

          nativeCanvas.restore()
        }
      }

      if (workspaceMode != WorkspaceMode.AR_TRACE) {
        drawIntoCanvas { canvas ->
          val nativeCanvas = canvas.nativeCanvas
          for (layer in layers) {
            if (!layer.isVisible) continue
            val layerBmp = drawingEngine.getLayerBitmap(layer.id) ?: continue

            val paint = android.graphics.Paint().apply {
              isAntiAlias = true
              alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
              when (layer.blendMode) {
                com.example.model.BlendModeType.MULTIPLY -> xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                com.example.model.BlendModeType.SCREEN -> xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
                com.example.model.BlendModeType.OVERLAY -> xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.OVERLAY)
                com.example.model.BlendModeType.DIFFERENCE -> xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DARKEN)
                com.example.model.BlendModeType.NORMAL -> {}
              }
            }

            val srcRect = android.graphics.Rect(0, 0, layerBmp.width, layerBmp.height)
            val dstRect = android.graphics.Rect(0, 0, w.toInt(), h.toInt())
            nativeCanvas.drawBitmap(layerBmp, srcRect, dstRect, paint)
          }
        }
      }

      val sStart = shapeStart
      val sCur = shapeCurrent
      if (sStart != null && sCur != null) {
        val shapeColor = Color(brushConfig.color).copy(alpha = brushConfig.opacity)
        when (shapeSubtype) {
          VectorShapeSubtype.LINE -> {
            drawLine(
              color = shapeColor,
              start = Offset(sStart.x, sStart.y),
              end = Offset(sCur.x, sCur.y),
              strokeWidth = brushConfig.size
            )
          }
          VectorShapeSubtype.RECTANGLE -> {
            val left = kotlin.math.min(sStart.x, sCur.x)
            val top = kotlin.math.min(sStart.y, sCur.y)
            val rw = kotlin.math.abs(sCur.x - sStart.x)
            val rh = kotlin.math.abs(sCur.y - sStart.y)
            drawRect(
              color = shapeColor,
              topLeft = Offset(left, top),
              size = androidx.compose.ui.geometry.Size(rw, rh),
              style = androidx.compose.ui.graphics.drawscope.Stroke(width = brushConfig.size)
            )
          }
          VectorShapeSubtype.CIRCLE -> {
            val cx = (sStart.x + sCur.x) * 0.5f
            val cy = (sStart.y + sCur.y) * 0.5f
            val rad = kotlin.math.hypot((sCur.x - sStart.x).toDouble(), (sCur.y - sStart.y).toDouble()).toFloat() * 0.5f
            drawCircle(
              color = shapeColor,
              radius = rad,
              center = Offset(cx, cy),
              style = androidx.compose.ui.graphics.drawscope.Stroke(width = brushConfig.size)
            )
          }
        }
      }
    }

    HudGridOverlay(
      gridType = hudGridType,
      gridOpacity = hudGridOpacity,
      symmetryMode = symmetryMode,
      modifier = Modifier.fillMaxSize()
    )

    if (isTorchRingOn) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .border(24.dp, WhitePure)
      )
    }
  }
}
