package com.libyuvresizer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibyuvResizerModuleScaleConstraintTest {

  private lateinit var module: LibyuvResizerModule
  private lateinit var reactContext: FakeReactContext
  private val createdFiles = mutableListOf<String>()

  @Before
  fun setUp() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    reactContext = FakeReactContext(ctx)
    module = LibyuvResizerModule(reactContext)
  }

  @After
  fun tearDown() {
    createdFiles.forEach { TestFixtures.deleteIfExists(it) }
    createdFiles.clear()
  }

  private fun resize(
    filePath: String,
    targetW: Double,
    targetH: Double,
    scaleConstraint: String
  ): FakePromise {
    val promise = FakePromise()
    module.resize(filePath, targetW, targetH, 80.0, 0.0, "contain", "", "box", scaleConstraint, promise)
    return promise
  }

  // onlyScaleDown: skip when target is larger than src (would upscale)

  @Test
  fun onlyScaleDown_targetLargerThanSrc_resolvesWithOriginalPath() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "small.jpg")
    createdFiles += src

    val promise = resize(src, 200.0, 200.0, "onlyScaleDown")

    assertTrue(promise.resolved)
    assertEquals(src, promise.result as String)
  }

  @Test
  fun onlyScaleDown_targetWidthLarger_resolvesWithOriginalPath() {
    // src 100x200, targetW=150 > srcW=100 → skip
    val src = TestFixtures.createJpeg(reactContext, 100, 200, "portrait.jpg")
    createdFiles += src

    val promise = resize(src, 150.0, 150.0, "onlyScaleDown")

    assertTrue(promise.resolved)
    assertEquals(src, promise.result as String)
  }

  @Test
  fun onlyScaleDown_targetHeightLarger_resolvesWithOriginalPath() {
    // src 200x100, targetH=150 > srcH=100 → skip
    val src = TestFixtures.createJpeg(reactContext, 200, 100, "landscape.jpg")
    createdFiles += src

    val promise = resize(src, 150.0, 150.0, "onlyScaleDown")

    assertTrue(promise.resolved)
    assertEquals(src, promise.result as String)
  }

  @Test
  fun onlyScaleDown_targetSmallerThanSrc_performsResize() {
    val src = TestFixtures.createJpeg(reactContext, 400, 300, "large.jpg")
    createdFiles += src

    val promise = resize(src, 200.0, 150.0, "onlyScaleDown")

    assertTrue(promise.resolved)
    val outPath = promise.result as String
    createdFiles += outPath
    assertFalse("should write new file, not return original", outPath == src)
    assertTrue(File(outPath).exists())
  }

  @Test
  fun onlyScaleDown_equalDims_performsResize() {
    val src = TestFixtures.createJpeg(reactContext, 200, 200, "equal.jpg")
    createdFiles += src

    val promise = resize(src, 200.0, 200.0, "onlyScaleDown")

    assertTrue(promise.resolved)
    val outPath = promise.result as String
    createdFiles += outPath
    assertFalse("equal dims should resize normally, not return original", outPath == src)
  }

  // onlyScaleUp: skip when target is smaller than src (would downscale)

  @Test
  fun onlyScaleUp_targetSmallerThanSrc_resolvesWithOriginalPath() {
    val src = TestFixtures.createJpeg(reactContext, 400, 300, "large2.jpg")
    createdFiles += src

    val promise = resize(src, 200.0, 150.0, "onlyScaleUp")

    assertTrue(promise.resolved)
    assertEquals(src, promise.result as String)
  }

  @Test
  fun onlyScaleUp_targetWidthSmaller_resolvesWithOriginalPath() {
    // src 400x200, targetW=300 < srcW=400 → skip
    val src = TestFixtures.createJpeg(reactContext, 400, 200, "wide.jpg")
    createdFiles += src

    val promise = resize(src, 300.0, 300.0, "onlyScaleUp")

    assertTrue(promise.resolved)
    assertEquals(src, promise.result as String)
  }

  @Test
  fun onlyScaleUp_targetLargerThanSrc_performsResize() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "tiny2.jpg")
    createdFiles += src

    val promise = resize(src, 200.0, 200.0, "onlyScaleUp")

    assertTrue(promise.resolved)
    val outPath = promise.result as String
    createdFiles += outPath
    assertFalse("should write new file, not return original", outPath == src)
    assertTrue(File(outPath).exists())
  }

  @Test
  fun onlyScaleUp_equalDims_performsResize() {
    val src = TestFixtures.createJpeg(reactContext, 200, 200, "equal2.jpg")
    createdFiles += src

    val promise = resize(src, 200.0, 200.0, "onlyScaleUp")

    assertTrue(promise.resolved)
    val outPath = promise.result as String
    createdFiles += outPath
    assertFalse("equal dims should resize normally, not return original", outPath == src)
  }

  // no constraint: normal behavior

  @Test
  fun noConstraint_alwaysPerformsResize() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "noconstraint.jpg")
    createdFiles += src

    val promise = resize(src, 200.0, 200.0, "")

    assertTrue(promise.resolved)
    val outPath = promise.result as String
    createdFiles += outPath
    assertFalse("no constraint should always resize", outPath == src)
  }

  // invalid constraint: rejected

  @Test
  fun invalidScaleConstraint_rejectsWithCode() {
    val src = TestFixtures.createJpeg(reactContext, 100, 100, "badconstraint.jpg")
    createdFiles += src

    val promise = resize(src, 50.0, 50.0, "both")

    assertTrue(promise.rejected)
    assertEquals("E_INVALID_SCALE_CONSTRAINT", promise.errorCode)
  }
}
