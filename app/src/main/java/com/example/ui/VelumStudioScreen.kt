package com.example.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.example.VelumViewModel
import com.example.camera.CameraManager
import com.example.ui.components.BottomScrubConsole
import com.example.ui.components.DrawingCanvasView
import com.example.ui.components.ExportModal
import com.example.ui.components.LeftToolRail
import com.example.ui.components.RightInspectorConsole
import com.example.ui.components.TopTelemetryHeader
import com.example.ui.components.TouchShieldOverlay
import com.example.ui.theme.VoidBackdrop

@Composable
fun VelumStudioScreen(
  viewModel: VelumViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val workspaceMode by viewModel.workspaceMode.collectAsState()
  val activeTool by viewModel.activeTool.collectAsState()
  val shapeSubtype by viewModel.shapeSubtype.collectAsState()
  val symmetryMode by viewModel.symmetryMode.collectAsState()
  val opticsConfig by viewModel.opticsConfig.collectAsState()
  val brushConfig by viewModel.brushConfig.collectAsState()
  val layers by viewModel.layers.collectAsState()
  val activeLayerId by viewModel.activeLayerId.collectAsState()
  val hudGridType by viewModel.hudGridType.collectAsState()
  val hudGridOpacity by viewModel.hudGridOpacity.collectAsState()
  val isInspectorCollapsed by viewModel.isInspectorCollapsed.collectAsState()
  val isLocked by viewModel.isLocked.collectAsState()
  val isTorchRingOn by viewModel.isTorchRingOn.collectAsState()
  val isMirrored by viewModel.isMirrored.collectAsState()
  val isCameraLive by viewModel.isCameraLive.collectAsState()
  val showExportModal by viewModel.showExportModal.collectAsState()
  val eyedropperColors by viewModel.eyedropperColors.collectAsState()
  val filteredReferenceBitmap by viewModel.filteredReferenceBitmap.collectAsState()
  val fallbackBitmap by viewModel.fallbackBitmap.collectAsState()
  val currentGhostOpacity by viewModel.currentGhostOpacity.collectAsState()
  val fps by viewModel.fps.collectAsState()
  val canUndo by viewModel.canUndo.collectAsState()
  val canRedo by viewModel.canRedo.collectAsState()

  val cameraManager = remember { CameraManager(context) }
  val previewView = remember { PreviewView(context) }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      cameraManager.initialize(lifecycleOwner, previewView) { live ->
        viewModel.setCameraLive(live)
      }
    } else {
      viewModel.setCameraLive(false)
    }
  }

  val pickMediaLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
          MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        viewModel.loadCustomReference(mutableBmp)
      } catch (e: Exception) {
      }
    }
  }

  LaunchedEffect(Unit) {
    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(VoidBackdrop)
  ) {
    DrawingCanvasView(
      workspaceMode = workspaceMode,
      activeTool = activeTool,
      shapeSubtype = shapeSubtype,
      symmetryMode = symmetryMode,
      brushConfig = brushConfig,
      opticsConfig = opticsConfig,
      onOpticsConfigChange = { viewModel.updateOpticsConfig(it) },
      currentGhostOpacity = currentGhostOpacity,
      layers = layers,
      activeLayerId = activeLayerId,
      drawingEngine = viewModel.drawingEngine,
      referenceBitmap = filteredReferenceBitmap,
      fallbackBitmap = fallbackBitmap,
      isCameraLive = isCameraLive,
      previewView = previewView,
      isTorchRingOn = isTorchRingOn,
      isMirrored = isMirrored,
      hudGridType = hudGridType,
      hudGridOpacity = hudGridOpacity,
      onColorSampled = { viewModel.addEyedropperColor(it) },
      onFrameDrawn = { viewModel.onCanvasFrameDrawn() },
      modifier = Modifier.fillMaxSize()
    )

    AnimatedVisibility(
      visible = !isLocked,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
    ) {
      TopTelemetryHeader(
        workspaceMode = workspaceMode,
        onWorkspaceModeChange = { viewModel.setWorkspaceMode(it) },
        fps = fps,
        zoomScale = opticsConfig.transform.scale,
        rotationDeg = opticsConfig.transform.rotation,
        activeLayerId = activeLayerId,
        isCameraLive = isCameraLive,
        isTorchOn = cameraManager.isTorchActive() || isTorchRingOn,
        isMirrored = isMirrored,
        onToggleFacing = {
          cameraManager.toggleFacing(lifecycleOwner, previewView)
        },
        onToggleTorch = {
          val hardwareWorked = cameraManager.toggleTorch()
          if (!hardwareWorked) {
            viewModel.toggleTorchRing()
          }
        },
        onToggleMirror = { viewModel.toggleMirror() },
        onPickImage = {
          pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onLockClick = { viewModel.setLocked(true) },
        onExportClick = { viewModel.setShowExportModal(true) }
      )
    }

    AnimatedVisibility(
      visible = !isLocked,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(start = 14.dp)
    ) {
      LeftToolRail(
        activeTool = activeTool,
        onSelectTool = { viewModel.setActiveTool(it) },
        shapeSubtype = shapeSubtype,
        onCycleShapeSubtype = { viewModel.cycleShapeSubtype() },
        symmetryMode = symmetryMode,
        onCycleSymmetryMode = { viewModel.cycleSymmetryMode() }
      )
    }

    AnimatedVisibility(
      visible = !isLocked,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 14.dp, top = 62.dp, bottom = 94.dp)
    ) {
      RightInspectorConsole(
        opticsConfig = opticsConfig,
        onOpticsConfigChange = { viewModel.updateOpticsConfig(it) },
        onSelectPreset = { viewModel.loadPreset(it) },
        brushConfig = brushConfig,
        onBrushConfigChange = { viewModel.updateBrushConfig(it) },
        eyedropperColors = eyedropperColors,
        layers = layers,
        activeLayerId = activeLayerId,
        onSelectLayer = { viewModel.selectLayer(it) },
        onUpdateLayer = { viewModel.updateLayer(it) },
        onClearLayer = { viewModel.clearActiveLayer(it) },
        onDuplicateLayer = { viewModel.duplicateLayer(it) },
        onMergeDown = { viewModel.mergeDownLayer(it) },
        hudGridType = hudGridType,
        onHudGridTypeChange = { viewModel.setHudGridType(it) },
        hudGridOpacity = hudGridOpacity,
        onHudGridOpacityChange = { viewModel.setHudGridOpacity(it) },
        isCollapsed = isInspectorCollapsed,
        onToggleCollapse = { viewModel.toggleInspectorCollapse() }
      )
    }

    AnimatedVisibility(
      visible = !isLocked,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 14.dp)
        .widthIn(max = 680.dp)
    ) {
      BottomScrubConsole(
        canUndo = canUndo,
        canRedo = canRedo,
        onUndo = { viewModel.undo() },
        onRedo = { viewModel.redo() },
        onClearActive = { viewModel.clearActiveLayer(activeLayerId) },
        ghostOpacity = opticsConfig.masterOpacity,
        onGhostOpacityChange = {
          viewModel.updateOpticsConfig(opticsConfig.copy(masterOpacity = it))
        },
        isStrobeEnabled = opticsConfig.strobeEnabled,
        onToggleStrobe = {
          viewModel.updateOpticsConfig(opticsConfig.copy(strobeEnabled = !opticsConfig.strobeEnabled))
        },
        strobeFrequency = opticsConfig.strobeFrequency,
        onStrobeFrequencyChange = {
          viewModel.updateOpticsConfig(opticsConfig.copy(strobeFrequency = it))
        }
      )
    }

    TouchShieldOverlay(
      isLocked = isLocked,
      onUnlock = { viewModel.setLocked(false) }
    )

    if (showExportModal) {
      ExportModal(
        layers = layers,
        referenceBitmap = filteredReferenceBitmap,
        referenceOpacity = currentGhostOpacity,
        drawingEngine = viewModel.drawingEngine,
        onDismiss = { viewModel.setShowExportModal(false) }
      )
    }
  }
}
