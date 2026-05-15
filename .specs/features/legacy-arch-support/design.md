# Legacy Architecture Support Design

**Spec**: `.specs/features/legacy-arch-support/spec.md`
**Status**: Draft

---

## Architecture Overview

The module must register itself on two separate paths and the JS layer must pick the right one at runtime.

```mermaid
graph TD
    A[JS: resize()] --> B{global.__turboModuleProxy?}
    B -- yes / New Arch --> C[TurboModuleRegistry.getEnforcing]
    B -- no  / Legacy  --> D[NativeModules.LibyuvResizer]
    C --> E[JSI / Codegen bridge]
    D --> F[Legacy NativeModules bridge]
    E --> G[LibyuvResizerModule.kt]
    F --> G
    G --> H[JNI / libyuv C++]
```

**Key invariant:** `LibyuvResizerModule.kt` does not change — it already extends the codegen class (`NativeLibyuvResizerSpec`) which itself extends `ReactContextBaseJavaModule`. The same Kotlin class serves both archs. Only the *package registration* and *JS bridge selection* change.

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
|---|---|---|
| `LibyuvResizerModule.kt` | `android/src/.../LibyuvResizerModule.kt` | No logic change; add `@ReactModule` annotation only |
| `NativeLibyuvResizer.ts` | `src/NativeLibyuvResizer.ts` | Replace last line's `getEnforcing` with compat guard |
| `LibyuvResizer.mm` | `ios/LibyuvResizer.mm` | Add `RCT_EXPORT_MODULE` + `RCT_EXPORT_METHOD` alongside existing turbo impl |
| `LibyuvResizer.h` | `ios/LibyuvResizer.h` | Add `RCTBridgeModule` protocol conformance |
| `android/build.gradle` | `android/build.gradle` | Add `buildConfigField` for `IS_NEW_ARCHITECTURE_ENABLED` |

### Integration Points

| System | Integration Method |
|---|---|
| `TurboReactPackage` (RN) | Swap `BaseReactPackage` → `TurboReactPackage`; provides `createNativeModules` automatically |
| `ReactModuleInfo` | Pass `BuildConfig.IS_NEW_ARCHITECTURE_ENABLED` to `isTurboModule` field |
| `NativeModules` (RN JS) | Fallback access path on legacy bridge |

---

## Components

### JS Bridge Compat Guard (`NativeLibyuvResizer.ts`)

- **Purpose**: Selects Turbo Module or NativeModules at runtime based on arch detection.
- **Location**: `src/NativeLibyuvResizer.ts`
- **Interfaces**:
  - Same `Spec` interface — no change.
  - Default export becomes the resolved module (typed as `Spec`).
- **Dependencies**: `react-native` (`NativeModules`, `TurboModuleRegistry`)
- **Reuses**: Existing `Spec` interface verbatim.

```ts
// Replace last line only
const isTurboModuleEnabled = global.__turboModuleProxy != null;

export default (
  isTurboModuleEnabled
    ? TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer')
    : NativeModules.LibyuvResizer
) as Spec;
```

> Note: If `NativeModules.LibyuvResizer` is `null` the call to `.resize(...)` will throw at call-site with a clear JS error. A null-guard with an explicit error message can be added for better DX (see T3).

---

### Android Package (`LibyuvResizerPackage.kt`)

- **Purpose**: Registers the module for both New Arch (via `getModule`) and Legacy Arch (via `createNativeModules` inherited from `TurboReactPackage`).
- **Location**: `android/src/main/java/com/libyuvresizer/LibyuvResizerPackage.kt`
- **Change**: `BaseReactPackage` → `TurboReactPackage` (same interface, `TurboReactPackage` adds `createNativeModules` for legacy).
- **Change**: `isTurboModule = true` → `isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED`.
- **Dependencies**: `com.facebook.react.TurboReactPackage`, `BuildConfig`
- **Reuses**: Existing `getModule` + `getReactModuleInfoProvider` bodies verbatim.

```kotlin
class LibyuvResizerPackage : TurboReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == LibyuvResizerModule.NAME) LibyuvResizerModule(reactContext) else null
  }

  override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
    mapOf(
      LibyuvResizerModule.NAME to ReactModuleInfo(
        LibyuvResizerModule.NAME,
        LibyuvResizerModule.NAME,
        canOverrideExistingModule = false,
        needsEagerInit = false,
        isCxxModule = false,
        isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      )
    )
  }
}
```

---

### Android Module Annotation (`LibyuvResizerModule.kt`)

- **Purpose**: Expose the module to the legacy bridge's module registry via `@ReactModule`.
- **Location**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
- **Change**: Add `@ReactModule(name = LibyuvResizerModule.NAME)` class annotation — zero logic change.
- **Reuses**: Entire existing implementation.

---

### Android Build Config (`android/build.gradle`)

- **Purpose**: Propagate the host app's `newArchEnabled` Gradle property into `BuildConfig` so the package registration reads it correctly.
- **Location**: `android/build.gradle`
- **Change**: Add `buildConfigField` inside `android { defaultConfig { ... } }`.

```gradle
buildConfigField(
  "boolean",
  "IS_NEW_ARCHITECTURE_ENABLED",
  project.hasProperty("newArchEnabled") ? project.getProperty("newArchEnabled") : "false"
)
```

---

### iOS Legacy Export (`LibyuvResizer.mm` + `LibyuvResizer.h`)

- **Purpose**: Register the module on the legacy `RCTBridge` so it loads without crashing.
- **Location**: `ios/LibyuvResizer.mm`, `ios/LibyuvResizer.h`
- **Changes**:
  - Header: add `<RCTBridgeModule>` to protocol conformance list.
  - `.mm`: add `RCT_EXPORT_MODULE(LibyuvResizer)` and `RCT_EXPORT_METHOD` macro wrapping the existing `reject(@"E_NOT_IMPLEMENTED", ...)` body.
- **Reuses**: Existing `getTurboModule` path (New Arch) unchanged.

```objc
// LibyuvResizer.h
@interface LibyuvResizer : NSObject <NativeLibyuvResizerSpec, RCTBridgeModule>
@end

// LibyuvResizer.mm (additions)
RCT_EXPORT_MODULE(LibyuvResizer)

RCT_EXPORT_METHOD(resize:(NSString *)filePath
                  targetWidth:(double)targetWidth
                  targetHeight:(double)targetHeight
                  quality:(double)quality
                  rotation:(double)rotation
                  mode:(NSString *)mode
                  outputPath:(NSString *)outputPath
                  filterMode:(NSString *)filterMode
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    reject(@"E_NOT_IMPLEMENTED", @"resize is not yet implemented on iOS", nil);
}
```

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
|---|---|---|
| `NativeModules.LibyuvResizer` is `null` (not linked) | Explicit `Error` thrown at call-site | Clear message: "native module not found, did you link?" |
| `getEnforcing` fails (turbo module not registered) | Existing RN behaviour (throws) | App-level crash — existing behaviour unchanged |
| Legacy arch host calls `resize` on iOS | `E_NOT_IMPLEMENTED` rejection | Same as current new-arch iOS behaviour |

---

## Tech Decisions

| Decision | Choice | Rationale |
|---|---|---|
| `TurboReactPackage` over manual `ReactPackage` impl | `TurboReactPackage` | Provides `createNativeModules` for free; `BaseReactPackage` is new-arch only |
| `BuildConfig.IS_NEW_ARCHITECTURE_ENABLED` | `project.hasProperty("newArchEnabled")` | Standard RN Gradle convention; host app sets `newArchEnabled=true` in `gradle.properties` |
| `global.__turboModuleProxy` check | Runtime check | Compile-time flag not available in JS; runtime check is the RN-recommended pattern |
| iOS: add `RCT_EXPORT_METHOD` wrapper | Yes | Module must respond to `resize` on legacy bridge even if it rejects; otherwise method not found error |
