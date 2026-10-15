package com.example.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraManager(private val context: Context) {

  private var cameraProvider: ProcessCameraProvider? = null
  private var camera: Camera? = null
  private var isTorchOn: Boolean = false
  private var isFrontFacing: Boolean = false

  fun initialize(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onStatusChanged: (Boolean) -> Unit
  ) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
      try {
        cameraProvider = cameraProviderFuture.get()
        bindCamera(lifecycleOwner, previewView)
        onStatusChanged(true)
      } catch (e: Exception) {
        onStatusChanged(false)
      }
    }, ContextCompat.getMainExecutor(context))
  }

  fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
    val provider = cameraProvider ?: return
    provider.unbindAll()

    val lensFacing = if (isFrontFacing) {
      CameraSelector.LENS_FACING_FRONT
    } else {
      CameraSelector.LENS_FACING_BACK
    }

    val cameraSelector = CameraSelector.Builder()
      .requireLensFacing(lensFacing)
      .build()

    val preview = Preview.Builder().build().also {
      it.setSurfaceProvider(previewView.surfaceProvider)
    }

    try {
      camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
      if (!isFrontFacing && camera?.cameraInfo?.hasFlashUnit() == true) {
        camera?.cameraControl?.enableTorch(isTorchOn)
      }
    } catch (e: Exception) {
      camera = null
    }
  }

  fun toggleFacing(lifecycleOwner: LifecycleOwner, previewView: PreviewView): Boolean {
    isFrontFacing = !isFrontFacing
    bindCamera(lifecycleOwner, previewView)
    return isFrontFacing
  }

  fun toggleTorch(): Boolean {
    val cam = camera ?: return false
    if (isFrontFacing || cam.cameraInfo.hasFlashUnit() != true) return false
    isTorchOn = !isTorchOn
    cam.cameraControl.enableTorch(isTorchOn)
    return isTorchOn
  }

  fun isTorchActive(): Boolean = isTorchOn
  fun isFront(): Boolean = isFrontFacing
}
