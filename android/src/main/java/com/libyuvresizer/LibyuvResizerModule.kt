package com.libyuvresizer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class LibyuvResizerModule(reactContext: ReactApplicationContext) :
    NativeLibyuvResizerSpec(reactContext) {

    override fun resize(
        filePath: String,
        targetWidth: Double,
        targetHeight: Double,
        quality: Double,
        rotation: Double,
        promise: Promise
    ) {
        try {
            if (!File(filePath).exists()) {
                promise.reject("E_FILE_NOT_FOUND", "File not found: $filePath")
                return
            }
            val dstW = targetWidth.toInt()
            val dstH = targetHeight.toInt()
            val q = quality.toInt()
            val rot = rotation.toInt()

            if (dstW <= 0 || dstH <= 0) {
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

            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val srcBitmap = BitmapFactory.decodeFile(filePath, opts)
                ?: run {
                    promise.reject("E_DECODE_FAILED", "Failed to decode image")
                    return
                }

            val dstBitmap = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
            try {
                if (rot == 0) {
                    nativeResize(srcBitmap, dstBitmap)
                } else {
                    nativeResizeAndRotate(srcBitmap, dstBitmap, rot)
                }
            } finally {
                srcBitmap.recycle()
            }

            val ext = if (q == 100) "png" else "jpg"
            val outFile = File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
            try {
                FileOutputStream(outFile).use { fos ->
                    val fmt = if (q == 100) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    dstBitmap.compress(fmt, q, fos)
                }
            } finally {
                dstBitmap.recycle()
            }

            promise.resolve(outFile.absolutePath)
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
