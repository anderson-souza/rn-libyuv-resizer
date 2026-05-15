# Project State

## Status

**Phase:** Android complete (resize + rotate + legacy bridge) — iOS native impl next
**Last updated:** 2026-05-14

## Active Work

- [x] Add libyuv git submodule
- [x] Create `android/CMakeLists.txt` with libyuv integration
- [x] Update `android/build.gradle` with `externalNativeBuild`
- [x] Design JS API for resize (`resize(filePath, w, h, quality, options?)`)
- [x] Implement Android native layer (JNI/C++ via `AndroidBitmap_lockPixels` + `libyuv::ARGBScale`)
- [x] Image rotation — Android (scale-first + `libyuv::ARGBRotate` via single `nativeResizeAndRotate` JNI call)
- [x] Custom output path — `outputPath?: string` in `ResizeOptions`; Android validates + saves; iOS stub added
- [x] Resize mode — `contain | cover | stretch` via `DimensionCalculator`
- [x] Filter mode — `none | linear | bilinear | box` mapped to libyuv filter enum
- [x] Extract `ResizeValidator` + `DimensionCalculator` as pure Kotlin (unit-testable without Android runtime)
- [x] Android JVM unit tests for validator and dimension calculator
- [x] Android instrumented tests for LibyuvResizerModule (integration, error, filter, outputPath, rotation)
- [x] 85% Jest coverage threshold enforced
- [x] Legacy bridge support — JS arch guard, Android `TurboReactPackage` + `@ReactModule`, iOS `RCT_EXPORT_METHOD`
- [ ] Implement iOS native layer (Accelerate/vImage)
- [ ] Image rotation — iOS (deferred; Android-only for now)

## Decisions

| Decision | Rationale |
|----------|-----------|
| Dual arch support (Turbo + Legacy Bridge) | Broaden adoption; some RN 0.68–0.72 projects can't migrate to New Arch yet |
| `TurboReactPackage` (not `BaseReactPackage`) | Provides `createNativeModules` needed by legacy bridge while keeping Turbo Module path |
| `@ReactModule` annotation | Required for legacy module registry discovery |
| `RCT_EXPORT_METHOD` on iOS alongside JSI | Single `.mm` satisfies both legacy and Turbo without duplication |
| JS arch guard via `globalThis.__turboModuleProxy` | Runtime check; no native build-time flag needed |
| `DimensionCalculator` + `ResizeValidator` extracted | Pure Kotlin, no Android runtime — JVM unit-testable without emulator |
| New Architecture Turbo Modules (primary) | Simpler codebase; JSI zero-copy; target audience uses modern RN |
| libyuv as git submodule (not pre-built AAR) | Architecture-specific SIMD optimizations (NEON, SVE2) at build time |
| iOS uses Accelerate, not libyuv | libyuv doesn't ship as iOS framework; Accelerate is on-device and zero-dependency |
| Android STL: `c++_shared` | Matches React Native's own NDK STL to avoid symbol conflicts |
| `AndroidBitmap_lockPixels` not ByteArray marshaling | Zero large copies over JNI; C++ gets raw pointer to Bitmap native memory |
| `ARGBScale` not `I420Scale` | Android Bitmap.ARGB_8888 is 4 bytes/pixel; no color-space conversion needed |
| `kFilterBox` default filter | Primary use case is downscale; box filter highest quality for shrinking |
| File encode/decode in Kotlin | Android codecs hardware-accelerated; keeps C++ stateless |
| `quality=100` → PNG, else JPEG | Lossless path available without separate API param |
| `outputPath=''` sentinel for absent param | TurboModule bridge requires fixed-arity positional args; no nullable strings |

## Blockers

None.

## Preferences

<!-- Track model-guidance tips already shown to avoid repeating -->
