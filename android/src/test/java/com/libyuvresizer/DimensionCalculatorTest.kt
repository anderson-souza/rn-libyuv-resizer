package com.libyuvresizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DimensionCalculatorTest {

  // --- calculateInSampleSize ---

  @Test
  fun `returns 1 when src smaller than dst`() {
    assertEquals(1, DimensionCalculator.calculateInSampleSize(100, 100, 200, 200))
  }

  @Test
  fun `returns 1 for equal dimensions`() {
    assertEquals(1, DimensionCalculator.calculateInSampleSize(200, 200, 200, 200))
  }

  @Test
  fun `returns 2 when src is double dst`() {
    assertEquals(2, DimensionCalculator.calculateInSampleSize(400, 400, 200, 200))
  }

  @Test
  fun `returns 4 when src is 4x dst`() {
    assertEquals(4, DimensionCalculator.calculateInSampleSize(800, 800, 200, 200))
  }

  @Test
  fun `returns 1 when only one axis exceeds but loop condition requires both`() {
    // srcW=400 > dstW=200, but srcH=100 < dstH=200 — loop exits at sampleSize=1
    assertEquals(1, DimensionCalculator.calculateInSampleSize(400, 100, 200, 200))
  }

  @Test
  fun `returns correct sampleSize for odd source dimensions`() {
    // srcW=401 srcH=401: halfW=halfH=200 — same as 400x400 case
    assertEquals(2, DimensionCalculator.calculateInSampleSize(401, 401, 200, 200))
  }

  // --- scaleBy ---

  @Test
  fun `scaleBy 2x doubles dimensions`() {
    assertEquals(Pair(200, 100), DimensionCalculator.scaleBy(2.0, 100.0, 50.0))
  }

  @Test
  fun `scaleBy 0_5 halves dimensions`() {
    assertEquals(Pair(50, 25), DimensionCalculator.scaleBy(0.5, 100.0, 50.0))
  }

  @Test
  fun `scaleBy minimum output is 1`() {
    assertEquals(Pair(1, 1), DimensionCalculator.scaleBy(0.0001, 1.0, 1.0))
  }

  @Test
  fun `scaleBy rounds correctly`() {
    // 100 * 0.056 = 5.6 -> 6
    assertEquals(Pair(6, 6), DimensionCalculator.scaleBy(0.056, 100.0, 100.0))
  }

  // --- computeDstDims: stretch ---

  @Test
  fun `stretch returns exact target dims`() {
    assertEquals(Pair(100, 50), DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 50, 0, "stretch"))
  }

  @Test
  fun `stretch ignores src dimensions`() {
    assertEquals(Pair(300, 200), DimensionCalculator.computeDstDims(10.0, 10.0, 300, 200, 0, "stretch"))
  }

  // --- computeDstDims: contain ---

  @Test
  fun `contain 16x9 src in square box constrains width`() {
    // 1920x1080 contain in 100x100: scale = min(100/1920, 100/1080) = 0.052... (width-limited)
    val (w, h) = DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 100, 0, "contain")
    assertEquals(100, w)
    assertEquals(56, h) // 1080 * (100/1920) ≈ 56.25 -> 56
  }

  @Test
  fun `contain portrait src in landscape box constrains height`() {
    // 100x200 contain in 200x100: scale = min(200/100, 100/200) = 0.5 (height-limited)
    val (w, h) = DimensionCalculator.computeDstDims(100.0, 200.0, 200, 100, 0, "contain")
    assertEquals(50, w) // 100 * 0.5
    assertEquals(100, h) // 200 * 0.5
  }

  @Test
  fun `contain output never exceeds target on either axis`() {
    val (w, h) = DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 100, 0, "contain")
    assertTrue("w=$w exceeds target", w <= 100)
    assertTrue("h=$h exceeds target", h <= 100)
  }

  @Test
  fun `contain never exceeds target for small integer dimensions`() {
    // exhaustive check over small src and target combos — previously broken with roundToInt
    for (sw in 1..20) for (sh in 1..20) for (tw in 1..20) for (th in 1..20) {
      val (w, h) = DimensionCalculator.computeDstDims(sw.toDouble(), sh.toDouble(), tw, th, 0, "contain")
      assertTrue("src=${sw}x${sh} target=${tw}x${th} → w=$w > tw=$tw", w <= tw)
      assertTrue("src=${sw}x${sh} target=${tw}x${th} → h=$h > th=$th", h <= th)
    }
  }

  // --- computeDstDims: cover ---

  @Test
  fun `cover 16x9 src in square box fills on both axes`() {
    // 1920x1080 cover 100x100: scale = max(100/1920, 100/1080) = 0.0926... (height-driven)
    val (w, h) = DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 100, 0, "cover")
    assertEquals(100, h)
    assertTrue("w=$w should exceed or equal target", w >= 100)
  }

  @Test
  fun `cover output meets target on at least one axis`() {
    val (w, h) = DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 100, 0, "cover")
    assertTrue("neither axis meets target: w=$w h=$h", w >= 100 || h >= 100)
  }

  @Test
  fun `cover 16x9 src in square box exact output width`() {
    // scale = max(100/1920, 100/1080) = 100/1080 = 0.09259...
    // w = round(1920 * 0.09259...) = round(177.77...) = 178
    // h = round(1080 * 0.09259...) = round(100.0) = 100
    val (w, h) = DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 100, 0, "cover")
    assertEquals(178, w)
    assertEquals(100, h)
  }

  // --- computeDstDims: rotation ---

  @Test
  fun `rotation 90 swaps effective dims for contain`() {
    // src 100x200, rotation=90 → effectiveW=200, effectiveH=100
    // contain in 100x100: scale = min(100/200, 100/100) = 0.5
    // result: scaleBy(0.5, 200, 100) = (100, 50)
    val (w, h) = DimensionCalculator.computeDstDims(100.0, 200.0, 100, 100, 90, "contain")
    assertEquals(100, w)
    assertEquals(50, h)
  }

  @Test
  fun `rotation 270 swaps effective dims same as 90`() {
    val result90 = DimensionCalculator.computeDstDims(100.0, 200.0, 100, 100, 90, "contain")
    val result270 = DimensionCalculator.computeDstDims(100.0, 200.0, 100, 100, 270, "contain")
    assertEquals(result90, result270)
  }

  @Test
  fun `rotation 0 does not swap dims for contain`() {
    // src 100x200, rotation=0 → effectiveW=100, effectiveH=200
    // contain in 100x100: scale = min(100/100, 100/200) = 0.5
    // result: scaleBy(0.5, 100, 200) = (50, 100)
    val (w, h) = DimensionCalculator.computeDstDims(100.0, 200.0, 100, 100, 0, "contain")
    assertEquals(50, w)
    assertEquals(100, h)
  }

  @Test
  fun `rotation 180 does not swap dims`() {
    val result0 = DimensionCalculator.computeDstDims(100.0, 200.0, 100, 100, 0, "contain")
    val result180 = DimensionCalculator.computeDstDims(100.0, 200.0, 100, 100, 180, "contain")
    assertEquals(result0, result180)
  }

  @Test
  fun `stretch ignores rotation`() {
    val result0 = DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 50, 0, "stretch")
    val result90 = DimensionCalculator.computeDstDims(1920.0, 1080.0, 100, 50, 90, "stretch")
    assertEquals(result0, result90)
  }
}
