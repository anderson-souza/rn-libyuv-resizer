package com.libyuvresizer

import kotlin.math.roundToInt

object DimensionCalculator {
  fun calculateInSampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
    var sampleSize = 1
    if (srcH > dstH || srcW > dstW) {
      val halfH = srcH / 2
      val halfW = srcW / 2
      while (halfH / sampleSize >= dstH && halfW / sampleSize >= dstW) sampleSize *= 2
    }
    return sampleSize
  }

  // floor=true uses toInt() to ensure contain mode never exceeds target dimensions
  fun scaleBy(scale: Double, w: Double, h: Double, floor: Boolean = false): Pair<Int, Int> {
    val round: (Double) -> Int = if (floor) Double::toInt else Double::roundToInt
    return Pair(maxOf(1, round(w * scale)), maxOf(1, round(h * scale)))
  }

  // For 90°/270°, srcW and srcH are the *raw bitmap* dims (before rotation).
  // effectiveW/H are swapped to represent the post-rotation logical dims used for scale math.
  fun computeDstDims(
    srcW: Double,
    srcH: Double,
    targetW: Int,
    targetH: Int,
    rotation: Int,
    mode: String
  ): Pair<Int, Int> {
    val (effectiveW, effectiveH) = if (rotation == 90 || rotation == 270)
      Pair(srcH, srcW)
    else
      Pair(srcW, srcH)

    return when (mode) {
      "stretch" -> Pair(targetW, targetH)
      "contain" -> scaleBy(
        minOf(targetW / effectiveW, targetH / effectiveH),
        effectiveW,
        effectiveH,
        floor = true
      )
      else -> scaleBy(
        maxOf(targetW / effectiveW, targetH / effectiveH),
        effectiveW,
        effectiveH
      )
    }
  }
}
