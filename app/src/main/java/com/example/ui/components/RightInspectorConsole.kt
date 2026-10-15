package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlendModeType
import com.example.model.BrushConfig
import com.example.model.ColorTint
import com.example.model.FilterMode
import com.example.model.HudGridType
import com.example.model.LayerItem
import com.example.model.OpticsConfig
import com.example.model.PresetStudy
import com.example.ui.theme.ConsoleSurfaceTranslucent
import com.example.ui.theme.ControlWell
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.StrobeWarning
import com.example.ui.theme.TelemetryText
import com.example.ui.theme.VoidBackdrop
import java.util.Locale

@Composable
fun RightInspectorConsole(
  opticsConfig: OpticsConfig,
  onOpticsConfigChange: (OpticsConfig) -> Unit,
  onSelectPreset: (PresetStudy) -> Unit,
  brushConfig: BrushConfig,
  onBrushConfigChange: (BrushConfig) -> Unit,
  eyedropperColors: List<Long>,
  layers: List<LayerItem>,
  activeLayerId: Int,
  onSelectLayer: (Int) -> Unit,
  onUpdateLayer: (LayerItem) -> Unit,
  onClearLayer: (Int) -> Unit,
  onDuplicateLayer: (Int) -> Unit,
  onMergeDown: (Int) -> Unit,
  hudGridType: HudGridType,
  onHudGridTypeChange: (HudGridType) -> Unit,
  hudGridOpacity: Float,
  onHudGridOpacityChange: (Float) -> Unit,
  isCollapsed: Boolean,
  onToggleCollapse: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(24.dp, 48.dp)
        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        .background(ConsoleSurfaceTranslucent)
        .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        .clickable { onToggleCollapse() }
        .testTag("inspector_collapse_handle"),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (isCollapsed) "◀" else "▶",
        fontSize = 10.sp,
        color = PrimaryAccent
      )
    }

    AnimatedVisibility(
      visible = !isCollapsed,
      enter = slideInHorizontally { it },
      exit = slideOutHorizontally { it }
    ) {
      Column(
        modifier = Modifier
          .width(288.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(12.dp))
          .background(ConsoleSurfaceTranslucent)
          .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
          .padding(8.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(VoidBackdrop)
            .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
            .padding(2.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          val tabLabels = listOf("OPTICS", "BRUSH", "LAYERS")
          tabLabels.forEachIndexed { index, label ->
            val isSelected = selectedTab == index
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) PrimaryAccent else Color.Transparent)
                .clickable { selectedTab = index }
                .testTag("inspector_tab_$index"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.sp,
                color = if (isSelected) VoidBackdrop else PrimaryText
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
          when (selectedTab) {
            0 -> OpticsTab(
              config = opticsConfig,
              onChange = onOpticsConfigChange,
              onSelectPreset = onSelectPreset
            )
            1 -> BrushTab(
              config = brushConfig,
              onChange = onBrushConfigChange,
              eyedropperColors = eyedropperColors
            )
            2 -> LayersTab(
              layers = layers,
              activeLayerId = activeLayerId,
              onSelectLayer = onSelectLayer,
              onUpdateLayer = onUpdateLayer,
              onClearLayer = onClearLayer,
              onDuplicateLayer = onDuplicateLayer,
              onMergeDown = onMergeDown,
              hudGridType = hudGridType,
              onHudGridTypeChange = onHudGridTypeChange,
              hudGridOpacity = hudGridOpacity,
              onHudGridOpacityChange = onHudGridOpacityChange
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OpticsTab(
  config: OpticsConfig,
  onChange: (OpticsConfig) -> Unit,
  onSelectPreset: (PresetStudy) -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    SectionHeader("OPTICAL FILTER MODE")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      FilterMode.values().take(3).forEach { mode ->
        ModeButton(
          label = mode.label,
          isSelected = config.filterMode == mode,
          modifier = Modifier.weight(1f),
          onClick = { onChange(config.copy(filterMode = mode)) }
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      FilterMode.values().drop(3).forEach { mode ->
        ModeButton(
          label = mode.label,
          isSelected = config.filterMode == mode,
          modifier = Modifier.weight(1f),
          onClick = { onChange(config.copy(filterMode = mode)) }
        )
      }
    }

    SectionHeader("CONVOLUTION PARAMETERS")
    CustomSlider(
      label = "EDGE THRESHOLD",
      value = config.edgeThreshold.toFloat(),
      range = 0f..255f,
      formatted = String.format(Locale.US, "%03d", config.edgeThreshold),
      onValueChange = { onChange(config.copy(edgeThreshold = it.toInt())) }
    )
    CustomSlider(
      label = "CONTRAST",
      value = config.contrast.toFloat(),
      range = -100f..100f,
      formatted = String.format(Locale.US, "%+04d", config.contrast),
      onValueChange = { onChange(config.copy(contrast = it.toInt())) }
    )
    CustomSlider(
      label = "BRIGHTNESS",
      value = config.brightness.toFloat(),
      range = -100f..100f,
      formatted = String.format(Locale.US, "%+04d", config.brightness),
      onValueChange = { onChange(config.copy(brightness = it.toInt())) }
    )
    CustomSlider(
      label = "SATURATION",
      value = config.saturation.toFloat(),
      range = 0f..200f,
      formatted = String.format(Locale.US, "%03d%%", config.saturation),
      onValueChange = { onChange(config.copy(saturation = it.toInt())) }
    )

    SectionHeader("LINE ART COLOR TINT")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      ColorTint.values().forEach { tint ->
        val isSelected = config.tint == tint
        Box(
          modifier = Modifier
            .weight(1f)
            .height(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) PrimaryAccent else ControlWell)
            .border(1.dp, if (isSelected) PrimaryAccent else HairlineBorder, RoundedCornerShape(4.dp))
          .clickable { onChange(config.copy(tint = tint)) },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = tint.label,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) VoidBackdrop else PrimaryText
          )
        }
      }
    }

    SectionHeader("REFERENCE TRANSFORM")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      TransformNudgeButton("-1°", Modifier.weight(1f)) {
        val t = config.transform
        onChange(config.copy(transform = t.copy(rotation = t.rotation - 1f)))
      }
      TransformNudgeButton("+1°", Modifier.weight(1f)) {
        val t = config.transform
        onChange(config.copy(transform = t.copy(rotation = t.rotation + 1f)))
      }
      TransformNudgeButton("90°", Modifier.weight(1f)) {
        val t = config.transform
        onChange(config.copy(transform = t.copy(rotation = (t.rotation + 90f) % 360f)))
      }
      TransformNudgeButton("RESET", Modifier.weight(1f)) {
        onChange(config.copy(transform = com.example.model.ReferenceTransform()))
      }
    }

    CustomSlider(
      label = "PERSPECTIVE TILT X",
      value = config.transform.tiltX,
      range = -45f..45f,
      formatted = String.format(Locale.US, "%+05.1f°", config.transform.tiltX),
      onValueChange = {
        val t = config.transform
        onChange(config.copy(transform = t.copy(tiltX = it)))
      }
    )
    CustomSlider(
      label = "PERSPECTIVE TILT Y",
      value = config.transform.tiltY,
      range = -45f..45f,
      formatted = String.format(Locale.US, "%+05.1f°", config.transform.tiltY),
      onValueChange = {
        val t = config.transform
        onChange(config.copy(transform = t.copy(tiltY = it)))
      }
    )

    SectionHeader("PROCEDURAL STUDY PRESETS")
    PresetStudy.values().forEach { preset ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(28.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(ControlWell)
          .border(1.dp, HairlineBorder, RoundedCornerShape(4.dp))
          .clickable { onSelectPreset(preset) }
          .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        Text(
          text = preset.label,
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
          color = OpticalCyan,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

@Composable
private fun BrushTab(
  config: BrushConfig,
  onChange: (BrushConfig) -> Unit,
  eyedropperColors: List<Long>
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    SectionHeader("STROKE ENGINE")
    CustomSlider(
      label = "SIZE",
      value = config.size,
      range = 1f..120f,
      formatted = String.format(Locale.US, "%03dPX", config.size.toInt()),
      onValueChange = { onChange(config.copy(size = it)) }
    )
    CustomSlider(
      label = "OPACITY",
      value = config.opacity * 100f,
      range = 1f..100f,
      formatted = String.format(Locale.US, "%03d%%", (config.opacity * 100).toInt()),
      onValueChange = { onChange(config.copy(opacity = it / 100f)) }
    )
    CustomSlider(
      label = "STABILIZER / STREAMLINE",
      value = config.stabilizer * 100f,
      range = 0f..95f,
      formatted = String.format(Locale.US, "%02d%%", (config.stabilizer * 100).toInt()),
      onValueChange = { onChange(config.copy(stabilizer = it / 100f)) }
    )
    CustomSlider(
      label = "HARDNESS / FLOW",
      value = config.hardness * 100f,
      range = 10f..100f,
      formatted = String.format(Locale.US, "%03d%%", (config.hardness * 100).toInt()),
      onValueChange = { onChange(config.copy(hardness = it / 100f)) }
    )

    SectionHeader("CURRENT INK COLOR")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(ControlWell)
        .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
        .padding(6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(Color(config.color))
          .border(1.dp, HairlineBorder, CircleShape)
      )
      Text(
        text = String.format(Locale.US, "#%06X", config.color and 0xFFFFFFL),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = PrimaryText
      )
    }

    SectionHeader("TRADITIONAL & INDUSTRIAL MEDIA")
    val swatches = listOf(
      0xFFFFFFFFL, 0xFF0D0E11L, 0xFFBC3926L, 0xFF1B365DL,
      0xFFC68B29L, 0xFF8A3324L, 0xFF00E5FFL, 0xFFFF4F00L,
      0xFF7C828FL, 0xFF4A6B82L, 0xFF5C4033L, 0xFFF5F2EBL,
      0xFF4B5320L, 0xFF8B5CF6L
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      swatches.take(7).forEach { hex ->
        SwatchCell(hex, config.color == hex) { onChange(config.copy(color = hex)) }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      swatches.drop(7).forEach { hex ->
        SwatchCell(hex, config.color == hex) { onChange(config.copy(color = hex)) }
      }
    }

    if (eyedropperColors.isNotEmpty()) {
      SectionHeader("LIVE CAMERA EYEDROPPER SLOTS")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        eyedropperColors.take(6).forEach { hex ->
          SwatchCell(hex, config.color == hex) { onChange(config.copy(color = hex)) }
        }
      }
    }
  }
}

@Composable
private fun LayersTab(
  layers: List<LayerItem>,
  activeLayerId: Int,
  onSelectLayer: (Int) -> Unit,
  onUpdateLayer: (LayerItem) -> Unit,
  onClearLayer: (Int) -> Unit,
  onDuplicateLayer: (Int) -> Unit,
  onMergeDown: (Int) -> Unit,
  hudGridType: HudGridType,
  onHudGridTypeChange: (HudGridType) -> Unit,
  hudGridOpacity: Float,
  onHudGridOpacityChange: (Float) -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    SectionHeader("4-LAYER COMPOSITOR")

    layers.reversed().forEach { layer ->
      val isActive = layer.id == activeLayerId

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(if (isActive) ControlWell else VoidBackdrop)
          .border(1.dp, if (isActive) PrimaryAccent else HairlineBorder, RoundedCornerShape(6.dp))
          .clickable { onSelectLayer(layer.id) }
          .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = layer.name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) PrimaryAccent else PrimaryText
          )

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SmallButton(if (layer.isVisible) "VIS" else "HID", layer.isVisible) {
              onUpdateLayer(layer.copy(isVisible = !layer.isVisible))
            }
            SmallButton(if (layer.isAlphaLocked) "LCK" else "UNL", layer.isAlphaLocked) {
              onUpdateLayer(layer.copy(isAlphaLocked = !layer.isAlphaLocked))
            }
            SmallButton("CLR", false) { onClearLayer(layer.id) }
            if (layer.id < 4) {
              SmallButton("DUP", false) { onDuplicateLayer(layer.id) }
            }
            if (layer.id > 1) {
              SmallButton("MRG", false) { onMergeDown(layer.id) }
            }
          }
        }

        CustomSlider(
          label = "OPACITY",
          value = layer.opacity * 100f,
          range = 0f..100f,
          formatted = String.format(Locale.US, "%03d%%", (layer.opacity * 100).toInt()),
          onValueChange = { onUpdateLayer(layer.copy(opacity = it / 100f)) }
        )
      }
    }

    SectionHeader("OPTICAL HUD GRID")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      HudGridType.values().take(3).forEach { grid ->
        ModeButton(
          label = grid.label,
          isSelected = hudGridType == grid,
          modifier = Modifier.weight(1f),
          onClick = { onHudGridTypeChange(grid) }
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      HudGridType.values().drop(3).forEach { grid ->
        ModeButton(
          label = grid.label,
          isSelected = hudGridType == grid,
          modifier = Modifier.weight(1f),
          onClick = { onHudGridTypeChange(grid) }
        )
      }
    }

    CustomSlider(
      label = "GRID OPACITY",
      value = hudGridOpacity * 100f,
      range = 10f..100f,
      formatted = String.format(Locale.US, "%03d%%", (hudGridOpacity * 100).toInt()),
      onValueChange = { onHudGridOpacityChange(it / 100f) }
    )
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    fontSize = 8.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.08.sp,
    color = TelemetryText
  )
}

@Composable
private fun ModeButton(
  label: String,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .height(28.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(if (isSelected) PrimaryAccent else ControlWell)
      .border(1.dp, if (isSelected) PrimaryAccent else HairlineBorder, RoundedCornerShape(4.dp))
      .clickable { onClick() }
      .padding(horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 7.sp,
      fontWeight = FontWeight.Bold,
      color = if (isSelected) VoidBackdrop else PrimaryText
    )
  }
}

@Composable
private fun TransformNudgeButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Box(
    modifier = modifier
      .height(26.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(ControlWell)
      .border(1.dp, HairlineBorder, RoundedCornerShape(4.dp))
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 8.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = PrimaryText
    )
  }
}

@Composable
private fun CustomSlider(
  label: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  formatted: String,
  onValueChange: (Float) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        fontSize = 8.sp,
        fontWeight = FontWeight.SemiBold,
        color = TelemetryText
      )
      Text(
        text = formatted,
        fontSize = 8.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        color = PrimaryText
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = range,
      colors = SliderDefaults.colors(
        thumbColor = PrimaryAccent,
        activeTrackColor = PrimaryAccent,
        inactiveTrackColor = ControlWell
      ),
      modifier = Modifier.height(20.dp)
    )
  }
}

@Composable
private fun SwatchCell(hex: Long, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .size(20.dp)
      .clip(RoundedCornerShape(3.dp))
      .background(Color(hex))
      .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) PrimaryAccent else HairlineBorder, RoundedCornerShape(3.dp))
      .clickable { onClick() }
  )
}

@Composable
private fun SmallButton(label: String, isActive: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(3.dp))
      .background(if (isActive) OpticalCyan else VoidBackdrop)
      .border(1.dp, HairlineBorder, RoundedCornerShape(3.dp))
      .clickable { onClick() }
      .padding(horizontal = 4.dp, vertical = 2.dp)
  ) {
    Text(
      text = label,
      fontSize = 7.sp,
      fontWeight = FontWeight.Bold,
      color = if (isActive) VoidBackdrop else PrimaryText
    )
  }
}
