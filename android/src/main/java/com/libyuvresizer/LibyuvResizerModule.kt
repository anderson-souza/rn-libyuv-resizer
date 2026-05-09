package com.libyuvresizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

class LibyuvResizerModule(reactContext: ReactApplicationContext) :
  NativeLibyuvResizerSpec(reactContext) {

  companion object {
    const val NAME = NativeLibyuvResizerSpec.NAME

    private val VALID_MODES = setOf("contain", "cover", "stretch")
    private val VALID_FILTER_MODES = setOf("none", "linear", "bilinear", "box")
    private val VALID_ROTATIONS = setOf(0, 90, 180, 270)
    private val FILTER_MODE_MAP = mapOf("none" to 0, "linear" to 1, "bilinear" to 2, "box" to 3)

    private fun calculateInSampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
      var sampleSize = 1
      if (srcH > dstH || srcW > dstW) {
        val halfH = srcH / 2
        val halfW = srcW / 2
        while (halfH / sampleSize >= dstH && halfW / sampleSize >= dstW) sampleSize *= 2
      }
      return sampleSize
    }

    private fun scaleBy(scale: Double, w: Double, h: Double): Pair<Int, Int> =
      Pair(maxOf(1, (w * scale).roundToInt()), maxOf(1, (h * scale).roundToInt()))

    init {
      System.loadLibrary("libyuvresizer")
    }
  }

  private external fun nativeResize(srcBitmap: Bitmap, dstBitmap: Bitmap, filterMode: Int)
  private external fun nativeResizeAndRotate(
    srcBitmap: Bitmap,
    dstBitmap: Bitmap,
    rotation: Int,
    filterMode: Int
  )

  override fun resize(
    filePath: String,
    targetWidth: Double,
    targetHeight: Double,
    quality: Double,
    rotation: Double,
    mode: String,
    outputPath: String,
    filterMode: String,
    promise: Promise
  ) {
    try {
      if (!File(filePath).exists()) {
        promise.reject("E_FILE_NOT_FOUND", "File not found: $filePath")
        return
      }

      val targetW = targetWidth.toInt()
      val targetH = targetHeight.toInt()
      val q = quality.toInt()
      val rot = rotation.toInt()

      if (targetW <= 0 || targetH <= 0) {
        promise.reject("E_INVALID_DIMS", "Invalid dimensions")
        return
      }
      if (q !in 1..100) {
        promise.reject("E_INVALID_QUALITY", "Quality must be between 1 and 100")
        return
      }
      if (rot !in VALID_ROTATIONS) {
        promise.reject("E_INVALID_ROTATION", "rotation must be 0, 90, 180 or 270, got: $rot")
        return
      }
      if (mode !in VALID_MODES) {
        promise.reject("E_INVALID_MODE", "mode must be contain, cover, or stretch, got: $mode")
        return
      }
      if (filterMode !in VALID_FILTER_MODES) {
        promise.reject(
          "E_INVALID_FILTER_MODE",
          "filterMode must be none, linear, bilinear, or box, got: $filterMode"
        )
        return
      }
      if (outputPath.isNotEmpty()) {
        val dir = File(outputPath)
        if (!dir.exists()) {
          promise.reject("E_INVALID_OUTPUT_PATH", "Output directory does not exist: $outputPath")
          return
        }
        if (!dir.isDirectory) {
          promise.reject(
            "E_INVALID_OUTPUT_PATH",
            "outputPath must be a directory, not a file: $outputPath"
          )
          return
        }
      }

      val bitmapConfig = Bitmap.Config.ARGB_8888

      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(filePath, boundsOpts)

      val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize =
          calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, targetW, targetH)
        inPreferredConfig = bitmapConfig
      }
      val srcBitmap = BitmapFactory.decodeFile(filePath, decodeOpts)
        ?: run {
          promise.reject("E_DECODE_FAILED", "Failed to decode image")
          return
        }

      // srcBitmap must be recycled even if dstBitmap allocation throws OOM
      try {
        val srcW = srcBitmap.width.toDouble()
        val srcH = srcBitmap.height.toDouble()

        // For 90°/270°, the logical image is transposed — swap effective dims for scale math
        val (effectiveW, effectiveH) = if (rot == 90 || rot == 270)
          Pair(srcH, srcW)
        else
          Pair(srcW, srcH)

        // cover scales to fill the target box on both axes; the output may exceed
        // targetW or targetH on one axis — no cropping is applied by design.
        val (dstW, dstH) = when (mode) {
          "stretch" -> Pair(targetW, targetH)
          "contain" -> scaleBy(
            minOf(targetW / effectiveW, targetH / effectiveH),
            effectiveW,
            effectiveH
          )

          else -> scaleBy(maxOf(targetW / effectiveW, targetH / effectiveH), effectiveW, effectiveH)
        }

        val dstBitmap = createBitmap(dstW, dstH, bitmapConfig)
        try {
          val filterModeInt = FILTER_MODE_MAP.getValue(filterMode)
          if (rot == 0) {
            nativeResize(srcBitmap, dstBitmap, filterModeInt)
          } else {
            nativeResizeAndRotate(srcBitmap, dstBitmap, rot, filterModeInt)
          }

          val ext = if (q == 100) "png" else "jpg"
          val outFile = resolveOutputFile(filePath, outputPath, ext)
          FileOutputStream(outFile).use { fos ->
            val fmt = if (q == 100) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            dstBitmap.compress(fmt, q, fos)
          }

          promise.resolve(outFile.absolutePath)
        } finally {
          dstBitmap.recycle()
        }
      } finally {
        srcBitmap.recycle()
      }
    } catch (e: Exception) {
      promise.reject("E_UNKNOWN", e.message ?: "Unknown error")
    }
  }

  private fun resolveOutputFile(inputFilePath: String, outputPath: String, ext: String): File {
    if (outputPath.isEmpty()) {
      return File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
    }
    return File(outputPath, File(inputFilePath).name)
  }

}
