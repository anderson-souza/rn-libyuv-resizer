# Architecture

**Pattern:** React Native Turbo Module library with legacy bridge fallback

## High-Level Structure

```
JS/TS (src/)
    │
    ├── NativeLibyuvResizer.ts  ← TurboModule spec (codegen source-of-truth)
    │       ↓ (codegen at build time)
    ├── resizer.native.tsx      ← validates args, calls NativeLibyuvResizer
    └── resizer.tsx             ← web/non-native fallback (throws)
                │
                ↓  (JSI bridge — zero-copy, no serialization)
        ┌───────────────────────────────────────────────────┐
        │  Android                                          │
        │  LibyuvResizerModule.kt                           │
        │    extends NativeLibyuvResizerSpec                │
        │    + @ReactModule for legacy bridge discovery     │
        │    → JNI → C++ (LibyuvResizerModule.cpp)         │
        │        → libyuv::ARGBScale / ARGBRotate           │
        │    DimensionCalculator.kt — pure resize math      │
        │    ResizeValidator.kt — input validation          │
        └───────────────────────────────────────────────────┘
        ┌───────────────────────────────────────────────────┐
        │  iOS                                              │
        │  LibyuvResizer.mm                                 │
        │    conforms NativeLibyuvResizerSpec (Turbo)       │
        │    + RCT_EXPORT_MODULE / RCT_EXPORT_METHOD (Legacy)│
        │    → Apple Accelerate vImage (planned)            │
        └───────────────────────────────────────────────────┘
```

## Identified Patterns

### Turbo Module Spec Pattern

**Location:** `src/NativeLibyuvResizer.ts`  
**Purpose:** Single source of truth for the native API contract; React Native codegen generates C++/Kotlin/ObjC++ bridge stubs.  
**Implementation:** `TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer')` — throws at runtime if native module not found. On legacy bridge, runtime-detects via `globalThis.__turboModuleProxy` and falls back to `NativeModules.LibyuvResizer`.

### Platform Split via `.native.tsx`

**Location:** `src/resizer.native.tsx` vs `src/resizer.tsx`  
**Purpose:** Metro bundler resolves `.native.*` for device, plain `.*` for web/Jest.  
**Implementation:** Native file validates args then calls `NativeLibyuvResizer`; web file throws a clear "native-only" error.

### Input Validation + Dimension Math (Pure Kotlin)

**Location:** `android/src/main/java/com/libyuvresizer/ResizeValidator.kt` and `DimensionCalculator.kt`  
**Purpose:** Extracted from `LibyuvResizerModule` to be independently unit-testable.  
**Implementation:** `ResizeValidator.validate(ResizeParams)` returns `ValidationResult.Valid | Invalid`. `DimensionCalculator.computeDstDims` handles `contain/cover/stretch` mode math and rotation-aware dimension swap.

### Package Registration (Android — Dual Arch)

**Location:** `android/LibyuvResizerPackage.kt`  
**Purpose:** Registers `LibyuvResizerModule` for both New Architecture and legacy bridge.  
**Implementation:** Extends `TurboReactPackage`; `isTurboModule` driven by `IS_NEW_ARCHITECTURE_ENABLED` buildConfigField from host app's Gradle property.

### iOS Dual Compatibility

**Location:** `ios/LibyuvResizer.mm`  
**Purpose:** Single `.mm` file works as both Turbo Module (JSI) and legacy bridge module.  
**Implementation:** `RCT_EXPORT_MODULE` + `RCT_EXPORT_METHOD` satisfy legacy bridge; `getTurboModule:` satisfies JSI. Both paths share the same implementation.

## Data Flow

### Resize Call (Android, current)

```
JS: resize(filePath, w, h, quality, options)
  → resizer.native.tsx: validate mode/filterMode, normalise rotation
  → JSI (or legacy bridge)
  → LibyuvResizerModule.kt:
      1. ResizeValidator.validate(params)
      2. BitmapFactory.decodeFile (with inSampleSize from DimensionCalculator)
      3. DimensionCalculator.computeDstDims → (dstW, dstH)
      4. JNI → C++ nativeResize or nativeResizeAndRotate
          → libyuv::ARGBScale + optional ARGBRotate
      5. FileOutputStream → JPEG/PNG encode
      6. promise.resolve(absolutePath)
```

### iOS (planned)

```
JS: resize(...)
  → JSI or RCT_EXPORT_METHOD
  → LibyuvResizer.mm → vImageScale_ARGB8888 → encode → resolve
```

## Code Organization

**Approach:** Flat, platform-split by extension  
**JS/TS:** `src/` — spec, platform impls, barrel export  
**Android:** `android/src/main/java/com/libyuvresizer/` — Kotlin module, package, validators  
**Android C++:** `android/src/main/cpp/` — JNI implementation calling libyuv  
**Android tests:** `android/src/test/` (JVM unit), `android/src/androidTest/` (instrumented)  
**iOS:** `ios/` — `.h` + `.mm` pair  
**Example app:** `example/` workspace — isolated RN app for manual testing
