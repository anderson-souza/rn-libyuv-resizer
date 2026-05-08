package com.libyuvresizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

class LibyuvResizerModule(reactContext: ReactApplicationContext) :
    NativeLibyuvResizerSpec(reactContext) {

    override fun resize(
        filePath: String,
        targetWidth: Double,
        targetHeight: Double,
        quality: Double,
        rotation: Double,
        mode: String,
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
            if (q < 1 || q > 100) {
                promise.reject("E_INVALID_QUALITY", "Quality must be between 1 and 100")
                return
            }
            if (rot !in setOf(0, 90, 180, 270)) {
                promise.reject("E_INVALID_ROTATION", "rotation must be 0, 90, 180 or 270, got: $rot")
                return
            }
            if (mode !in setOf("contain", "cover", "stretch")) {
                promise.reject("E_INVALID_MODE", "mode must be contain, cover, or stretch, got: $mode")
                return
            }

            val decodeOpts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
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
                    else -> {
                        val scale = if (mode == "contain")
                            minOf(targetW / effectiveW, targetH / effectiveH)
                        else
                            maxOf(targetW / effectiveW, targetH / effectiveH)
                        Pair(
                            maxOf(1, (effectiveW * scale).roundToInt()),
                            maxOf(1, (effectiveH * scale).roundToInt())
                        )
                    }
                }

                val dstBitmap = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
                try {
                    if (rot == 0) {
                        nativeResize(srcBitmap, dstBitmap)
                    } else {
                        nativeResizeAndRotate(srcBitmap, dstBitmap, rot)
                    }

                    val ext = if (q == 100) "png" else "jpg"
                    val outFile = File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
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

    private external fun nativeResize(srcBitmap: Bitmap, dstBitmap: Bitmap)
    private external fun nativeResizeAndRotate(srcBitmap: Bitmap, dstBitmap: Bitmap, rotation: Int)

    companion object {
        const val NAME = NativeLibyuvResizerSpec.NAME

        init {
            System.loadLibrary("libyuvresizer")
        }
    }
}
