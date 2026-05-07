package com.libyuvresizer

import com.facebook.react.bridge.ReactApplicationContext

class LibyuvResizerModule(reactContext: ReactApplicationContext) :
  NativeLibyuvResizerSpec(reactContext) {

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = NativeLibyuvResizerSpec.NAME
  }
}
