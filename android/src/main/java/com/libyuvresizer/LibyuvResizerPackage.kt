package com.libyuvresizer

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class LibyuvResizerPackage : TurboReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == LibyuvResizerModule.NAME) {
      LibyuvResizerModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
    mapOf(
      LibyuvResizerModule.NAME to ReactModuleInfo(
        name = LibyuvResizerModule.NAME,
        className = LibyuvResizerModule.NAME,
        canOverrideExistingModule = false,
        needsEagerInit = false,
        isCxxModule = false,
        isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      )
    )
  }
}
