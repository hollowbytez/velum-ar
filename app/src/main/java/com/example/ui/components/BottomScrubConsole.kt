package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ConsoleSurfaceTranslucent
import com.example.ui.theme.ControlWell
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.StrobeWarning
import com.example.ui.theme.TelemetryText
import com.example.ui.theme.VoidBackdrop
import java.util.Locale

@Composable
fun BottomScrubConsole(
  canUndo: Boolean,
  canRedo: Boolean,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onClearActive: () -> Unit,
  ghostOpacity: Float,
  onGhostOpacityChange: (Float) -> Unit,
  isStrobeEnabled: Boolean,
  onToggleStrobe: () -> Unit,
  strobeFrequency: Float,
  onStrobeFrequencyChange: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .height(64.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(ConsoleSurfaceTranslucent)
      .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      TactileConsoleButton(
        label = "UNDO",
        isEnabled = canUndo,
        onClick = onUndo,
        tag = "undo_button"
      )
      TactileConsoleButton(
        label = "REDO",
        isEnabled = canRedo,
        onClick = onRedo,
        tag = "redo_button"
      )
      TactileConsoleButton(
        label = "CLEAR",
        isEnabled = true,
        onClick = onClearActive,
        tag = "clear_active_button"
      )
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "GHOST REFERENCE OPACITY",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.08.sp,
          color = TelemetryText
        )
        Text(
          text = String.format(Locale.US, "%03d%%", (ghostOpacity * 100).toInt()),
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = PrimaryText
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(28.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
          val ticks = 21
          val step = size.width / (ticks - 1)
          for (i in 0 until ticks) {
            val x = i * step
            val isMajor = i % 5 == 0
            val tickH = if (isMajor) 8f else 4f
            val color = if (isMajor) Color(0xFF7C828F) else Color(0xFF23262E)
            drawLine(color, Offset(x, 0f), Offset(x, tickH), strokeWidth = 1f)
          }
        }

        Slider(
          value = ghostOpacity * 100f,
          onValueChange = { onGhostOpacityChange(it / 100f) },
          valueRange = 0f..100f,
          colors = SliderDefaults.colors(
            thumbColor = PrimaryAccent,
            activeTrackColor = PrimaryAccent,
            inactiveTrackColor = Color.Transparent
          ),
          modifier = Modifier.fillMaxWidth().testTag("ghost_opacity_slider")
        )
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(if (isStrobeEnabled) StrobeWarning else ControlWell)
          .border(1.dp, if (isStrobeEnabled) StrobeWarning else HairlineBorder, RoundedCornerShape(6.dp))
          .clickable { onToggleStrobe() }
          .padding(horizontal = 8.dp)
          .testTag("strobe_toggle_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "STROBE",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.06.sp,
          color = if (isStrobeEnabled) VoidBackdrop else PrimaryText
        )
      }

      Row(
        modifier = Modifier
          .height(34.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(VoidBackdrop)
          .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
          .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf(0.5f, 1.0f, 2.0f).forEach { freq ->
          val isSelected = isStrobeEnabled && strobeFrequency == freq
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSelected) StrobeWarning else Color.Transparent)
              .clickable { onStrobeFrequencyChange(freq) }
              .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${freq}Hz",
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = if (isSelected) VoidBackdrop else TelemetryText
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TactileConsoleButton(
  label: String,
  isEnabled: Boolean,
  onClick: () -> Unit,
  tag: String
) {
  Box(
    modifier = Modifier
      .height(34.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(if (isEnabled) ControlWell else VoidBackdrop)
      .border(1.dp, if (isEnabled) HairlineBorder else Color(0xFF181A1F), RoundedCornerShape(6.dp))
      .clickable(enabled = isEnabled) { onClick() }
      .padding(horizontal = 10.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.06.sp,
      color = if (isEnabled) PrimaryText else Color(0xFF484E59)
    )
  }
}
