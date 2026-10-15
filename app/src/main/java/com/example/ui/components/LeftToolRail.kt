package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.model.VectorShapeSubtype
import com.example.ui.theme.ConsoleSurfaceTranslucent
import com.example.ui.theme.ControlWell
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.VoidBackdrop

@Composable
fun LeftToolRail(
  activeTool: ToolType,
  onSelectTool: (ToolType) -> Unit,
  shapeSubtype: VectorShapeSubtype,
  onCycleShapeSubtype: () -> Unit,
  symmetryMode: SymmetryMode,
  onCycleSymmetryMode: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .width(52.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(ConsoleSurfaceTranslucent)
      .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
      .padding(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    ToolRailButton(
      tool = ToolType.TRANSFORM,
      isActive = activeTool == ToolType.TRANSFORM,
      onClick = { onSelectTool(ToolType.TRANSFORM) }
    )
    ToolRailButton(
      tool = ToolType.INK_PEN,
      isActive = activeTool == ToolType.INK_PEN,
      onClick = { onSelectTool(ToolType.INK_PEN) }
    )
    ToolRailButton(
      tool = ToolType.GRAIN_BRUSH,
      isActive = activeTool == ToolType.GRAIN_BRUSH,
      onClick = { onSelectTool(ToolType.GRAIN_BRUSH) }
    )
    ToolRailButton(
      tool = ToolType.CHISEL_MARKER,
      isActive = activeTool == ToolType.CHISEL_MARKER,
      onClick = { onSelectTool(ToolType.CHISEL_MARKER) }
    )
    ToolRailButton(
      tool = ToolType.VECTOR_SHAPE,
      isActive = activeTool == ToolType.VECTOR_SHAPE,
      badgeText = shapeSubtype.label,
      onClick = {
        if (activeTool == ToolType.VECTOR_SHAPE) {
          onCycleShapeSubtype()
        } else {
          onSelectTool(ToolType.VECTOR_SHAPE)
        }
      }
    )
    ToolRailButton(
      tool = ToolType.ERASER,
      isActive = activeTool == ToolType.ERASER,
      onClick = { onSelectTool(ToolType.ERASER) }
    )
    ToolRailButton(
      tool = ToolType.EYEDROPPER,
      isActive = activeTool == ToolType.EYEDROPPER,
      onClick = { onSelectTool(ToolType.EYEDROPPER) }
    )
    ToolRailButton(
      tool = ToolType.SYMMETRY_AXIS,
      isActive = activeTool == ToolType.SYMMETRY_AXIS || symmetryMode != SymmetryMode.OFF,
      badgeText = if (symmetryMode != SymmetryMode.OFF) symmetryMode.label else null,
      onClick = {
        onCycleSymmetryMode()
        onSelectTool(ToolType.SYMMETRY_AXIS)
      }
    )
  }
}

@Composable
private fun ToolRailButton(
  tool: ToolType,
  isActive: Boolean,
  badgeText: String? = null,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isActive) PrimaryAccent else ControlWell)
      .border(1.dp, if (isActive) PrimaryAccent else HairlineBorder, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .testTag("tool_${tool.key.lowercase()}"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(18.dp)) {
      val iconColor = if (isActive) VoidBackdrop else PrimaryText
      val stroke = Stroke(width = 1.75f)

      when (tool) {
        ToolType.TRANSFORM -> drawTransformIcon(iconColor, stroke)
        ToolType.INK_PEN -> drawPenIcon(iconColor, stroke)
        ToolType.GRAIN_BRUSH -> drawBrushIcon(iconColor, stroke)
        ToolType.CHISEL_MARKER -> drawChiselIcon(iconColor, stroke)
        ToolType.VECTOR_SHAPE -> drawShapeIcon(iconColor, stroke)
        ToolType.ERASER -> drawEraserIcon(iconColor, stroke)
        ToolType.EYEDROPPER -> drawEyedropperIcon(iconColor, stroke)
        ToolType.SYMMETRY_AXIS -> drawSymmetryIcon(iconColor, stroke)
      }
    }

    if (badgeText != null) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(1.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(OpticalCyan)
          .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = badgeText.take(3),
          fontSize = 6.sp,
          color = VoidBackdrop,
          lineHeight = 7.sp
        )
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTransformIcon(color: Color, stroke: Stroke) {
  val w = size.width
  val h = size.height
  val cx = w * 0.5f
  val cy = h * 0.5f

  drawLine(color, Offset(cx - 7f, cy), Offset(cx + 7f, cy), strokeWidth = 1.5f)
  drawLine(color, Offset(cx, cy - 7f), Offset(cx, cy + 7f), strokeWidth = 1.5f)

  drawRect(color, Offset(1f, 1f), Size(4f, 4f))
  drawRect(color, Offset(w - 5f, 1f), Size(4f, 4f))
  drawRect(color, Offset(1f, h - 5f), Size(4f, 4f))
  drawRect(color, Offset(w - 5f, h - 5f), Size(4f, 4f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPenIcon(color: Color, stroke: Stroke) {
  val path = Path().apply {
    moveTo(size.width * 0.5f, 1f)
    lineTo(size.width - 2f, size.height - 4f)
    lineTo(size.width * 0.5f, size.height - 1f)
    lineTo(2f, size.height - 4f)
    close()
  }
  drawPath(path, color, style = stroke)
  drawLine(color, Offset(size.width * 0.5f, 6f), Offset(size.width * 0.5f, size.height - 1f), strokeWidth = 1.5f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBrushIcon(color: Color, stroke: Stroke) {
  val path = Path().apply {
    moveTo(2f, size.height - 3f)
    lineTo(size.width * 0.5f, 2f)
    lineTo(size.width - 2f, size.height - 3f)
  }
  drawPath(path, color, style = stroke)

  for (i in 0..4) {
    val x = 4f + i * 2.5f
    drawLine(color, Offset(x, size.height - 5f), Offset(x, size.height - 1f), strokeWidth = 1.2f)
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChiselIcon(color: Color, stroke: Stroke) {
  val path = Path().apply {
    moveTo(2f, 2f)
    lineTo(size.width - 4f, 2f)
    lineTo(size.width - 2f, size.height - 2f)
    lineTo(4f, size.height - 2f)
    close()
  }
  drawPath(path, color, style = stroke)
  drawLine(color, Offset(2f, 2f), Offset(size.width - 2f, size.height - 2f), strokeWidth = 1.5f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShapeIcon(color: Color, stroke: Stroke) {
  drawRect(color, Offset(1f, 1f), Size(10f, 10f), style = stroke)
  drawCircle(color, radius = 5f, center = Offset(size.width - 5f, size.height - 5f), style = stroke)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEraserIcon(color: Color, stroke: Stroke) {
  val path = Path().apply {
    moveTo(3f, size.height - 4f)
    lineTo(size.width * 0.4f, 2f)
    lineTo(size.width - 2f, size.height * 0.6f)
    lineTo(size.width * 0.6f, size.height - 2f)
    close()
  }
  drawPath(path, color, style = stroke)
  drawLine(color, Offset(size.width * 0.35f, size.height * 0.7f), Offset(size.width * 0.7f, size.height * 0.35f), strokeWidth = 1.2f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEyedropperIcon(color: Color, stroke: Stroke) {
  val path = Path().apply {
    moveTo(size.width - 3f, 2f)
    lineTo(size.width - 2f, 5f)
    lineTo(7f, size.height - 4f)
    lineTo(2f, size.height - 2f)
    lineTo(4f, size.height - 7f)
    lineTo(size.width - 5f, 2f)
    close()
  }
  drawPath(path, color, style = stroke)
  drawCircle(color, radius = 1.5f, center = Offset(3f, size.height - 3f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSymmetryIcon(color: Color, stroke: Stroke) {
  drawLine(color, Offset(size.width * 0.5f, 1f), Offset(size.width * 0.5f, size.height - 1f), strokeWidth = 1.5f)
  drawCircle(color, radius = 2.5f, center = Offset(3.5f, size.height * 0.5f))
  drawCircle(color, radius = 2.5f, center = Offset(size.width - 3.5f, size.height * 0.5f))
}
