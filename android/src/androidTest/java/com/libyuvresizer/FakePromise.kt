package com.libyuvresizer

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap

class FakePromise : Promise {
  var result: Any? = null
    private set
  var errorCode: String? = null
    private set
  var errorMessage: String? = null
    private set

  private var _resolved = false
  val resolved: Boolean get() = _resolved
  val rejected: Boolean get() = errorCode != null

  override fun resolve(value: Any?) {
    result = value
    _resolved = true
  }

  override fun reject(code: String?, message: String?) {
    errorCode = code
    errorMessage = message
  }

  override fun reject(code: String?, throwable: Throwable?) {
    errorCode = code
    errorMessage = throwable?.message
  }

  override fun reject(code: String?, message: String?, throwable: Throwable?) {
    errorCode = code
    errorMessage = message
  }

  override fun reject(code: String?, userInfo: WritableMap) {
    errorCode = code
  }

  override fun reject(code: String?, throwable: Throwable?, userInfo: WritableMap) {
    errorCode = code
    errorMessage = throwable?.message
  }

  override fun reject(code: String?, message: String?, userInfo: WritableMap) {
    errorCode = code
    errorMessage = message
  }

  override fun reject(code: String?, message: String?, throwable: Throwable?, userInfo: WritableMap?) {
    errorCode = code
    errorMessage = message
  }

  override fun reject(throwable: Throwable) {
    errorCode = "E_UNKNOWN"
    errorMessage = throwable.message
  }

  override fun reject(throwable: Throwable, userInfo: WritableMap) {
    errorCode = "E_UNKNOWN"
    errorMessage = throwable.message
  }

  override fun reject(message: String) {
    errorCode = "E_UNKNOWN"
    errorMessage = message
  }
}
