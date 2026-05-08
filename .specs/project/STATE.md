# Project State

## Status

**Phase:** Android implementation complete — iOS next
**Last updated:** 2026-05-06

## Active Work

- [x] Add libyuv git submodule
- [x] Create `android/CMakeLists.txt` with libyuv integration
- [x] Update `android/build.gradle` with `externalNativeBuild`
- [x] Design JS API for resize
- [x] Implement Android native layer (JNI/C++ via `AndroidBitmap_lockPixels` + `libyuv::ARGBScale`)
- [x] Image rotation — Android (scale-first + `libyuv::ARGBRotate` via single `nativeResizeAndRotate` JNI call)
- [ ] Implement iOS native layer (Accelerate/vImage)
- [ ] Image rotation — iOS (deferred; Android-only for now)

## Decisions

| Decision | Rationale |
|----------|-----------|
| New Architecture only (Turbo Modules) | Simpler codebase; no bridge compat layer needed; target audience uses modern RN |
| libyuv as git submodule (not pre-built AAR) | Allows architecture-specific SIMD optimizations (NEON, SVE2) at build time |
| iOS uses Accelerate, not libyuv | libyuv doesn't ship as an iOS framework; Accelerate is on-device and zero-dependency |
| Android STL: `c++_shared` | Matches React Native's own NDK STL to avoid symbol conflicts |
| `AndroidBitmap_lockPixels` not ByteArray marshaling | Zero large copies over JNI; C++ gets raw pointer to Bitmap native memory |
| `ARGBScale` not `I420Scale` | Android Bitmap.ARGB_8888 is 4 bytes/pixel; no color-space conversion needed |
| `kFilterBox` filter | Primary use case is downscale; box filter highest quality for shrinking |
| File encode/decode in Kotlin | Android codecs hardware-accelerated; keeps C++ stateless |
| quality=100 → PNG, else JPEG | Lossless path available without separate API param |

## Blockers

None.

## Preferences

<!-- Track model-guidance tips already shown to avoid repeating -->
