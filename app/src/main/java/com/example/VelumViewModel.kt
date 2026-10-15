package com.example

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphics.DrawingEngine
import com.example.graphics.OpticalFilterEngine
import com.example.model.BrushConfig
import com.example.model.ColorTint
import com.example.model.FilterMode
import com.example.model.HudGridType
import com.example.model.LayerItem
import com.example.model.OpticsConfig
import com.example.model.PresetStudy
import com.example.model.ReferenceTransform
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.model.VectorShapeSubtype
import com.example.model.WorkspaceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

class VelumViewModel : ViewModel() {

  val drawingEngine = DrawingEngine(1080, 1080)

  private val _workspaceMode = MutableStateFlow(WorkspaceMode.HYBRID_AR)
  val workspaceMode: StateFlow<WorkspaceMode> = _workspaceMode.asStateFlow()

  private val _activeTool = MutableStateFlow(ToolType.INK_PEN)
  val activeTool: StateFlow<ToolType> = _activeTool.asStateFlow()

  private val _shapeSubtype = MutableStateFlow(VectorShapeSubtype.LINE)
  val shapeSubtype: StateFlow<VectorShapeSubtype> = _shapeSubtype.asStateFlow()

  private val _symmetryMode = MutableStateFlow(SymmetryMode.OFF)
  val symmetryMode: StateFlow<SymmetryMode> = _symmetryMode.asStateFlow()

  private val _opticsConfig = MutableStateFlow(
    OpticsConfig(
      filterMode = FilterMode.SOBEL_EDGE,
      edgeThreshold = 110,
      contrast = 20,
      brightness = 0,
      saturation = 100,
      tint = ColorTint.CYAN,
      masterOpacity = 0.80f
    )
  )
  val opticsConfig: StateFlow<OpticsConfig> = _opticsConfig.asStateFlow()

  private val _brushConfig = MutableStateFlow(
    BrushConfig(
      size = 10f,
      opacity = 1.0f,
      stabilizer = 0.50f,
      hardness = 0.90f,
      color = 0xFFECEEF2L
    )
  )
  val brushConfig: StateFlow<BrushConfig> = _brushConfig.asStateFlow()

  private val _layers = MutableStateFlow(
    listOf(
      LayerItem(1, "L1 // ROUGH", opacity = 0.9f),
      LayerItem(2, "L2 // CONTOUR", opacity = 1.0f),
      LayerItem(3, "L3 // INK DETAIL", opacity = 1.0f),
      LayerItem(4, "L4 // HIGHLIGHTS", opacity = 1.0f)
    )
  )
  val layers: StateFlow<List<LayerItem>> = _layers.asStateFlow()

  private val _activeLayerId = MutableStateFlow(2)
  val activeLayerId: StateFlow<Int> = _activeLayerId.asStateFlow()

  private val _hudGridType = MutableStateFlow(HudGridType.NONE)
  val hudGridType: StateFlow<HudGridType> = _hudGridType.asStateFlow()

  private val _hudGridOpacity = MutableStateFlow(0.40f)
  val hudGridOpacity: StateFlow<Float> = _hudGridOpacity.asStateFlow()

  private val _isInspectorCollapsed = MutableStateFlow(false)
  val isInspectorCollapsed: StateFlow<Boolean> = _isInspectorCollapsed.asStateFlow()

  private val _isLocked = MutableStateFlow(false)
  val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

  private val _isTorchRingOn = MutableStateFlow(false)
  val isTorchRingOn: StateFlow<Boolean> = _isTorchRingOn.asStateFlow()

  private val _isMirrored = MutableStateFlow(false)
  val isMirrored: StateFlow<Boolean> = _isMirrored.asStateFlow()

  private val _isCameraLive = MutableStateFlow(false)
  val isCameraLive: StateFlow<Boolean> = _isCameraLive.asStateFlow()

  private val _showExportModal = MutableStateFlow(false)
  val showExportModal: StateFlow<Boolean> = _showExportModal.asStateFlow()

  private val _eyedropperColors = MutableStateFlow(listOf<Long>())
  val eyedropperColors: StateFlow<List<Long>> = _eyedropperColors.asStateFlow()

  private val _rawReferenceBitmap = MutableStateFlow<Bitmap?>(null)
  private val _filteredReferenceBitmap = MutableStateFlow<Bitmap?>(null)
  val filteredReferenceBitmap: StateFlow<Bitmap?> = _filteredReferenceBitmap.asStateFlow()

  private val _fallbackBitmap = MutableStateFlow<Bitmap?>(null)
  val fallbackBitmap: StateFlow<Bitmap?> = _fallbackBitmap.asStateFlow()

  private val _currentGhostOpacity = MutableStateFlow(0.80f)
  val currentGhostOpacity: StateFlow<Float> = _currentGhostOpacity.asStateFlow()

  private val _fps = MutableStateFlow(60)
  val fps: StateFlow<Int> = _fps.asStateFlow()

  private val _canUndo = MutableStateFlow(false)
  val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

  private val _canRedo = MutableStateFlow(false)
  val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

  private var filterJob: Job? = null
  private var strobeJob: Job? = null
  private var fpsFrameCount = 0
  private var lastFpsTimestamp = System.currentTimeMillis()

  init {
    loadPreset(PresetStudy.SKULL_STUDY)
    _fallbackBitmap.value = OpticalFilterEngine.createCalibrationViewfinder()
    startStrobeLoop()
  }

  fun setWorkspaceMode(mode: WorkspaceMode) {
    _workspaceMode.value = mode
  }

  fun setActiveTool(tool: ToolType) {
    _activeTool.value = tool
  }

  fun cycleShapeSubtype() {
    val values = VectorShapeSubtype.values()
    val nextIndex = (_shapeSubtype.value.ordinal + 1) % values.size
    _shapeSubtype.value = values[nextIndex]
  }

  fun cycleSymmetryMode() {
    val values = SymmetryMode.values()
    val nextIndex = (_symmetryMode.value.ordinal + 1) % values.size
    _symmetryMode.value = values[nextIndex]
  }

  fun updateOpticsConfig(config: OpticsConfig) {
    _opticsConfig.value = config
    _currentGhostOpacity.value = config.masterOpacity
    reprocessFilter()
  }

  fun updateBrushConfig(config: BrushConfig) {
    _brushConfig.value = config
  }

  fun selectLayer(id: Int) {
    _activeLayerId.value = id
  }

  fun updateLayer(item: LayerItem) {
    _layers.value = _layers.value.map { if (it.id == item.id) item else it }
  }

  fun clearActiveLayer(id: Int) {
    drawingEngine.clearLayer(id)
    updateUndoRedoStates()
  }

  fun duplicateLayer(sourceId: Int) {
    val targetId = if (sourceId < 4) sourceId + 1 else 1
    drawingEngine.duplicateLayer(sourceId, targetId)
    updateUndoRedoStates()
  }

  fun mergeDownLayer(upperId: Int) {
    if (upperId > 1) {
      drawingEngine.mergeDown(upperId, upperId - 1)
      _activeLayerId.value = upperId - 1
      updateUndoRedoStates()
    }
  }

  fun undo() {
    drawingEngine.undo(_activeLayerId.value)
    updateUndoRedoStates()
  }

  fun redo() {
    drawingEngine.redo(_activeLayerId.value)
    updateUndoRedoStates()
  }

  fun onCanvasFrameDrawn() {
    updateUndoRedoStates()
    recordFpsFrame()
  }

  private fun updateUndoRedoStates() {
    _canUndo.value = drawingEngine.canUndo()
    _canRedo.value = drawingEngine.canRedo()
  }

  fun setHudGridType(type: HudGridType) {
    _hudGridType.value = type
  }

  fun setHudGridOpacity(opacity: Float) {
    _hudGridOpacity.value = opacity
  }

  fun toggleInspectorCollapse() {
    _isInspectorCollapsed.value = !_isInspectorCollapsed.value
  }

  fun setLocked(locked: Boolean) {
    _isLocked.value = locked
  }

  fun toggleTorchRing() {
    _isTorchRingOn.value = !_isTorchRingOn.value
  }

  fun toggleMirror() {
    _isMirrored.value = !_isMirrored.value
  }

  fun setCameraLive(live: Boolean) {
    _isCameraLive.value = live
  }

  fun setShowExportModal(show: Boolean) {
    _showExportModal.value = show
  }

  fun addEyedropperColor(color: Long) {
    val list = _eyedropperColors.value.toMutableList()
    if (!list.contains(color)) {
      list.add(0, color)
      if (list.size > 6) list.removeAt(list.size - 1)
      _eyedropperColors.value = list
    }
    _brushConfig.value = _brushConfig.value.copy(color = color)
  }

  fun loadPreset(study: PresetStudy) {
    viewModelScope.launch(Dispatchers.Default) {
      val bmp = OpticalFilterEngine.createPresetBitmap(study)
      _rawReferenceBitmap.value = bmp
      val filtered = OpticalFilterEngine.applyFilters(bmp, _opticsConfig.value)
      withContext(Dispatchers.Main) {
        _filteredReferenceBitmap.value = filtered
      }
    }
  }

  fun loadCustomReference(bitmap: Bitmap) {
    viewModelScope.launch(Dispatchers.Default) {
      _rawReferenceBitmap.value = bitmap
      val filtered = OpticalFilterEngine.applyFilters(bitmap, _opticsConfig.value)
      withContext(Dispatchers.Main) {
        _filteredReferenceBitmap.value = filtered
      }
    }
  }

  private fun reprocessFilter() {
    val raw = _rawReferenceBitmap.value ?: return
    filterJob?.cancel()
    filterJob = viewModelScope.launch(Dispatchers.Default) {
      val processed = OpticalFilterEngine.applyFilters(raw, _opticsConfig.value)
      withContext(Dispatchers.Main) {
        _filteredReferenceBitmap.value = processed
      }
    }
  }

  private fun startStrobeLoop() {
    strobeJob?.cancel()
    strobeJob = viewModelScope.launch(Dispatchers.Default) {
      val startTime = System.currentTimeMillis()
      while (isActive) {
        val config = _opticsConfig.value
        if (config.strobeEnabled) {
          val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0f
          val wave = (sin(2.0 * PI * config.strobeFrequency * elapsedSec) * 0.5f + 0.5f).toFloat()
          val oscillating = 0.15f + wave * 0.70f
          _currentGhostOpacity.value = oscillating
        } else {
          _currentGhostOpacity.value = config.masterOpacity
        }
        delay(33)
      }
    }
  }

  private fun recordFpsFrame() {
    fpsFrameCount++
    val now = System.currentTimeMillis()
    if (now - lastFpsTimestamp >= 1000) {
      _fps.value = (fpsFrameCount * 1000L / (now - lastFpsTimestamp)).toInt()
      fpsFrameCount = 0
      lastFpsTimestamp = now
    }
  }
}
