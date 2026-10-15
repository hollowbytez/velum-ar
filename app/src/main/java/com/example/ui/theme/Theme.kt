package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VelumColorScheme = darkColorScheme(
  primary = PrimaryAccent,
  onPrimary = VoidBackdrop,
  primaryContainer = ControlWell,
  onPrimaryContainer = PrimaryText,
  secondary = OpticalCyan,
  onSecondary = VoidBackdrop,
  tertiary = StrobeWarning,
  onTertiary = VoidBackdrop,
  background = VoidBackdrop,
  onBackground = PrimaryText,
  surface = ConsoleSurface,
  onSurface = PrimaryText,
  surfaceVariant = ControlWell,
  onSurfaceVariant = TelemetryText,
  outline = HairlineBorder
)

@Composable
fun VelumTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = VelumColorScheme,
    typography = Typography,
    content = content
  )
}
