package com.libyuvresizer

import android.content.Context
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.CatalystInstance
import com.facebook.react.bridge.JavaScriptContextHolder
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UIManager
import com.facebook.react.turbomodule.core.interfaces.CallInvokerHolder

@Suppress("UNCHECKED_CAST")
class FakeReactContext(context: Context) : ReactApplicationContext(context) {

  override fun <T : JavaScriptModule> getJSModule(jsInterface: Class<T>): T =
    throw UnsupportedOperationException()

  override fun <T : NativeModule> hasNativeModule(nativeModuleInterface: Class<T>): Boolean = false

  override fun getNativeModules(): MutableCollection<NativeModule> = mutableListOf()

  override fun <T : NativeModule> getNativeModule(nativeModuleInterface: Class<T>): T? = null

  override fun getNativeModule(moduleName: String): NativeModule? = null

  override fun getCatalystInstance(): CatalystInstance =
    throw UnsupportedOperationException()

  override fun hasActiveCatalystInstance(): Boolean = false

  override fun hasActiveReactInstance(): Boolean = false

  override fun hasCatalystInstance(): Boolean = false

  override fun hasReactInstance(): Boolean = false

  override fun destroy() {}

  override fun handleException(e: Exception) {
    throw RuntimeException(e)
  }

  override fun isBridgeless(): Boolean = false

  override fun getJavaScriptContextHolder(): JavaScriptContextHolder? = null

  override fun getJSCallInvokerHolder(): CallInvokerHolder? = null

  override fun getFabricUIManager(): UIManager? = null

  override fun getSourceURL(): String? = null

  override fun registerSegment(segmentId: Int, path: String, callback: Callback) {}
}
