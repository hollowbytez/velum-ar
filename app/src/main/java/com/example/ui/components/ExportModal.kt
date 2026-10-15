package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.graphics.DrawingEngine
import com.example.model.LayerItem
import com.example.ui.theme.ConsoleSurface
import com.example.ui.theme.ControlWell
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.OpticalCyan
import com.example.ui.theme.PrimaryAccent
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.TelemetryText
import com.example.ui.theme.VoidBackdrop
import java.io.File
import java.io.FileOutputStream

@Composable
fun ExportModal(
  layers: List<LayerItem>,
  referenceBitmap: Bitmap?,
  referenceOpacity: Float,
  drawingEngine: DrawingEngine,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var exportMode by remember { mutableStateOf(0) }

  val previewBitmap = remember(exportMode, layers, referenceBitmap) {
    when (exportMode) {
      0 -> drawingEngine.exportComposite(
        layers = layers,
        referenceBitmap = referenceBitmap,
        referenceOpacity = referenceOpacity,
        includeReference = true,
        includeBackdrop = true
      )
      1 -> drawingEngine.exportComposite(
        layers = layers,
        referenceBitmap = null,
        referenceOpacity = 0f,
        includeReference = false,
        includeBackdrop = false
      )
      else -> {
        referenceBitmap?.copy(Bitmap.Config.ARGB_8888, true) ?: drawingEngine.exportComposite(
          layers = layers,
          referenceBitmap = null,
          referenceOpacity = 0f,
          includeReference = false,
          includeBackdrop = true
        )
      }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(440.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(ConsoleSurface)
        .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "EXPORT STUDIO SNAPSHOT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.sp,
            color = PrimaryText
          )
          Text(
            text = "PRECISION HIGH-RESOLUTION PNG RENDER",
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            color = TelemetryText
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ControlWell)
            .border(1.dp, HairlineBorder, RoundedCornerShape(4.dp))
            .clickable { onDismiss() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "✕", fontSize = 11.sp, color = PrimaryText)
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(VoidBackdrop)
          .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
          .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        listOf("COMPOSITE", "ARTWORK ONLY", "STENCIL SHEET").forEachIndexed { idx, label ->
          val isSelected = exportMode == idx
          Box(
            modifier = Modifier
              .weight(1f)
              .height(30.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSelected) PrimaryAccent else Color.Transparent)
              .clickable { exportMode = idx },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) VoidBackdrop else PrimaryText
            )
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(VoidBackdrop)
          .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        Image(
          bitmap = previewBitmap.asImageBitmap(),
          contentDescription = "Export Preview",
          modifier = Modifier.padding(4.dp),
          contentScale = ContentScale.Fit
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${previewBitmap.width} × ${previewBitmap.height} PX // 32-BIT ARGB",
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace,
          color = OpticalCyan
        )

        Box(
          modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PrimaryAccent)
            .clickable {
              shareBitmap(context, previewBitmap)
              onDismiss()
            }
            .padding(horizontal = 16.dp)
            .testTag("confirm_export_share_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "SAVE & SHARE PNG",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.sp,
            color = VoidBackdrop
          )
        }
      }
    }
  }
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
  try {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()
    val file = File(cachePath, "velum_export_${System.currentTimeMillis()}.png")
    val stream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.flush()
    stream.close()

    val contentUri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "image/png"
      putExtra(Intent.EXTRA_STREAM, contentUri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share VELUM Studio Artwork"))
  } catch (e: Exception) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, "Exported artwork from VELUM Optical Studio.")
    }
    context.startActivity(Intent.createChooser(shareIntent, "VELUM"))
  }
}
