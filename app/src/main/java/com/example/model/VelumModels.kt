package com.example.model

import android.graphics.Bitmap

enum class WorkspaceMode(val label: String) {
  AR_TRACE("AR TRACE"),
  HYBRID_AR("HYBRID AR"),
  SCREEN_STUDIO("SCREEN STUDIO")
}

enum class ToolType(val key: String, val label: String) {
  TRANSFORM("V", "TRANSFORM"),
  INK_PEN("P", "INK PEN"),
  GRAIN_BRUSH("B", "GRAIN BRUSH"),
  CHISEL_MARKER("M", "CHISEL"),
  VECTOR_SHAPE("L", "SHAPE"),
  ERASER("E", "ERASER"),
  EYEDROPPER("I", "EYEDROPPER"),
  SYMMETRY_AXIS("S", "SYMMETRY")
}

enum class VectorShapeSubtype(val label: String) {
  LINE("LINE"),
  RECTANGLE("RECT"),
  CIRCLE("CIRCLE")
}

enum class SymmetryMode(val label: String) {
  OFF("OFF"),
  VERTICAL("VERT"),
  HORIZONTAL("HORIZ"),
  QUAD("QUAD"),
  RADIAL_8("RADIAL 8")
}

enum class FilterMode(val label: String) {
  RAW("RAW"),
  SOBEL_EDGE("SOBEL EDGE"),
  STENCIL_THRESHOLD("STENCIL THRESHOLD"),
  LUMA_INVERT("LUMA INVERT"),
  POSTERIZE("POSTERIZE"),
  MONO_CHROMATIC("MONO CHROMATIC")
}

enum class ColorTint(val label: String, val hex: Long) {
  NONE("NATURAL", 0x00000000),
  CYAN("CYAN", 0xFF00E5FF),
  VERMILION("VERMILION", 0xFFFF4F00),
  EMERALD("EMERALD", 0xFF10B981)
}

enum class HudGridType(val label: String) {
  NONE("NONE"),
  RULE_OF_THIRDS("THIRDS"),
  ARCHITECTURAL_16("16x16 GRID"),
  ISOMETRIC_30("ISOMETRIC 30°"),
  GOLDEN_SPIRAL("GOLDEN SPIRAL"),
  TWO_POINT_PERSPECTIVE("2-POINT PERSPECTIVE")
}

enum class PresetStudy(val label: String) {
  SKULL_STUDY("ANATOMY SKULL"),
  ARCHITECTURE("PERSPECTIVE ARCH"),
  BOTANICAL("BOTANICAL SPECIMEN"),
  PORTRAIT_PROPORTIONS("PORTRAIT RATIOS")
}

enum class BlendModeType(val label: String) {
  NORMAL("NORMAL"),
  MULTIPLY("MULTIPLY"),
  SCREEN("SCREEN"),
  OVERLAY("OVERLAY"),
  DIFFERENCE("DIFFERENCE")
}

data class LayerItem(
  val id: Int,
  val name: String,
  val opacity: Float = 1.0f,
  val isVisible: Boolean = true,
  val isAlphaLocked: Boolean = false,
  val blendMode: BlendModeType = BlendModeType.NORMAL
)

data class BrushConfig(
  val size: Float = 12f,
  val opacity: Float = 1.0f,
  val stabilizer: Float = 0.55f,
  val hardness: Float = 0.85f,
  val color: Long = 0xFFECEEF2
)

data class ReferenceTransform(
  val panX: Float = 0f,
  val panY: Float = 0f,
  val scale: Float = 1.0f,
  val rotation: Float = 0f,
  val tiltX: Float = 0f,
  val tiltY: Float = 0f
)

data class OpticsConfig(
  val filterMode: FilterMode = FilterMode.SOBEL_EDGE,
  val edgeThreshold: Int = 110,
  val contrast: Int = 15,
  val brightness: Int = 0,
  val saturation: Int = 100,
  val tint: ColorTint = ColorTint.CYAN,
  val transform: ReferenceTransform = ReferenceTransform(),
  val masterOpacity: Float = 0.75f,
  val strobeEnabled: Boolean = false,
  val strobeFrequency: Float = 1.0f
)

data class PointRecord(
  val x: Float,
  val y: Float,
  val pressure: Float = 1.0f,
  val time: Long = System.currentTimeMillis()
)
