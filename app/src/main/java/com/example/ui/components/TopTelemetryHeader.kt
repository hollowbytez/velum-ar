package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WorkspaceMode
import com.example.ui.theme.ConsoleSurfaceTranslucent
import com.example.ui.theme.ControlWell
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.TelemetryText
import com.example.ui.theme.VoidBackdrop
import java.util.Locale

@Composable
fun TopTelemetryHeader(
  workspaceMode: WorkspaceMode,
  onWorkspaceModeChange: (WorkspaceMode) -> Unit,
  fps: Int,
  zoomScale: Float,
  rotationDeg: Float,
  activeLayerId: Int,
  isCameraLive: Boolean,
  isTorchOn: Boolean,
  isMirrored: Boolean,
  onToggleFacing: () -> Unit,
  onToggleTorch: () -> Unit,
  onToggleMirror: () -> Unit,
  onPickImage: () -> Unit,
  onLockClick: () -> Unit,
  onExportClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .background(ConsoleSurfaceTranslucent)
      .border(1.dp, HairlineBorder)
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      VelumLogoMark(modifier = Modifier.size(24.dp))

      Column(modifier = Modifier.padding(end = 4.dp)) {
        Text(
          text = "VELUM",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.12.sp,
          color = PrimaryText,
          lineHeight = 14.sp
        )
        Text(
          text = "OPTICAL STUDIO // 01",
          fontSize = 8.sp,
          fontWeight = FontWeight.Medium,
          letterSpacing = 0.08.sp,
          color = TelemetryText,
          lineHeight = 10.sp
        )
      }

      Row(
        modifier = Modifier
          .height(30.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(VoidBackdrop)
          .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
          .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        WorkspaceMode.values().forEach { mode ->
          val isSelected = mode == workspaceMode
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSelected) PrimaryAccent else Color.Transparent)
              .clickable { onWorkspaceModeChange(mode) }
              .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = mode.label,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.06.sp,
              color = if (isSelected) VoidBackdrop else PrimaryText
            )
          }
        }
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      TelemetryPill(label = "FPS", value = String.format(Locale.US, "%03d", fps))
      TelemetryPill(label = "ZOOM", value = String.format(Locale.US, "%03d%%", (zoomScale * 100).toInt()))
      TelemetryPill(label = "ROT", value = String.format(Locale.US, "%+05.1f°", rotationDeg))
      TelemetryPill(label = "L", value = "L$activeLayerId")
      TelemetryPill(
        label = "CAM",
        value = if (isCameraLive) "ONLINE" else "SIMULATED",
        accentColor = if (isCameraLive) OpticalCyan else TelemetryText
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      HeaderIconButton(label = "REF", onClick = onPickImage, tag = "upload_ref_button")
      HeaderIconButton(label = "CAM", onClick = onToggleFacing, tag = "camera_facing_button")
      HeaderIconButton(
        label = "FLIP",
        onClick = onToggleMirror,
        isActive = isMirrored,
        tag = "mirror_button"
      )
      HeaderIconButton(
        label = "LIGHT",
        onClick = onToggleTorch,
        isActive = isTorchOn,
        activeColor = Color(0xFFFFB800),
        tag = "torch_button"
      )
      HeaderIconButton(
        label = "LOCK",
        onClick = onLockClick,
        tag = "lock_screen_button"
      )

      Box(
        modifier = Modifier
          .height(30.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(PrimaryAccent)
          .clickable { onExportClick() }
          .padding(horizontal = 12.dp)
          .testTag("export_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "EXPORT",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.08.sp,
          color = VoidBackdrop
        )
      }
    }
  }
}

@Composable
private fun VelumLogoMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f

    val octagon = Path().apply {
      val r = w * 0.44f
      val inset = r * 0.4142f
      moveTo(cx - inset, cy - r)
      lineTo(cx + inset, cy - r)
      lineTo(cx + r, cy - inset)
      lineTo(cx + r, cy + inset)
      lineTo(cx + inset, cy + r)
      lineTo(cx - inset, cy + r)
      lineTo(cx - r, cy + inset)
      lineTo(cx - r, cy - inset)
      close()
    }
    drawPath(octagon, Color(0xFFE2E4E9), style = Stroke(width = 1.5f))

    val crossColor = Color(0xFF7C828F)
    drawLine(crossColor, Offset(cx - 7f, cy), Offset(cx + 7f, cy), strokeWidth = 1.2f)
    drawLine(crossColor, Offset(cx, cy - 7f), Offset(cx, cy + 7f), strokeWidth = 1.2f)

    drawCircle(Color(0xFFFF4F00), radius = 2.5f, center = Offset(cx, cy))
  }
}

@Composable
private fun TelemetryPill(
  label: String,
  value: String,
  accentColor: Color = PrimaryText
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(ControlWell)
      .border(1.dp, HairlineBorder, RoundedCornerShape(4.dp))
      .padding(horizontal = 6.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = label,
      fontSize = 8.sp,
      fontWeight = FontWeight.SemiBold,
      color = TelemetryText,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = value,
      fontSize = 9.sp,
      fontWeight = FontWeight.Medium,
      color = accentColor,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
private fun HeaderIconButton(
  label: String,
  onClick: () -> Unit,
  isActive: Boolean = false,
  activeColor: Color = PrimaryAccent,
  tag: String
) {
  Box(
    modifier = Modifier
      .height(30.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(if (isActive) activeColor else ControlWell)
      .border(1.dp, if (isActive) activeColor else HairlineBorder, RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .padding(horizontal = 8.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.08.sp,
      color = if (isActive) VoidBackdrop else PrimaryText
    )
  }
}
