# Roadmap

**Current Milestone:** M1 — Native Foundation  
**Status:** In Progress

---

## M1 — Native Foundation

**Goal:** Working native build with libyuv wired up on Android and Accelerate on iOS — no public API yet, just verified compilation and JNI call round-trip  
**Target:** libyuv submodule added, CMake builds green, placeholder native call works end-to-end

### Features

**libyuv Android Integration** - IN PROGRESS

- Add libyuv as git submodule (`android.googlesource.com/platform/external/libyuv`)
- Create `android/CMakeLists.txt` with `add_subdirectory` + link `yuv` target
- Update `android/build.gradle` with `externalNativeBuild` + NDK flags
- Verify CMake build compiles for all 4 ABIs (armeabi-v7a, x86, x86_64, arm64-v8a)

**iOS Accelerate Baseline** - PLANNED

- Add `Accelerate` framework to podspec
- Implement stub `vImage` resize call in `LibyuvResizer.mm`
- Verify pod install + Xcode build passes

---

## M2 — Resize API

**Goal:** Public `resizeImage` API usable from JS — accepts URI, target dimensions, returns URI of resized image  
**Target:** API documented, both platforms implemented, basic tests passing

### Features

**JS/TS API Contract** - PLANNED

- Define `resizeImage(uri: string, width: number, height: number): Promise<string>` in `NativeLibyuvResizer.ts`
- Add web fallback (throws with clear message)
- Export from `index.tsx`

**Android Resize Implementation** - PLANNED

- Decode input image to pixel buffer (ARGB/YUV) in Kotlin
- Pass buffer to C++ via JNI
- Call `libyuv::ARGBScale` (or `I420Scale` for YUV input)
- Encode result, write to temp file, resolve promise with URI

**iOS Resize Implementation** - PLANNED

- Decode input URI to `vImage_Buffer`
- Call `vImageScale_ARGB8888`
- Encode to JPEG, write to temp file, resolve promise with URI

**Unit Tests (JS layer)** - PLANNED

- Test that web fallback throws
- Test that native module is called with correct args (mock)

---

## M3 — Rotate API

**Goal:** Public `rotateImage` API — accepts URI + degrees (0/90/180/270), returns URI  
**Target:** Both platforms implemented, integrated with example app

### Features

**JS/TS API Contract** - PLANNED

- Define `rotateImage(uri: string, degrees: 0 | 90 | 180 | 270): Promise<string>` in spec
- Export from `index.tsx`

**Android Rotate Implementation** - PLANNED

- Map degrees → `libyuv::RotationMode` enum (`kRotate90`, `kRotate180`, `kRotate270`)
- Call `libyuv::ARGBRotate`
- Write result, resolve promise

**iOS Rotate Implementation** - PLANNED

- Map degrees → `vImageRotate90_ARGB8888` or matrix rotation via vImage
- Write result, resolve promise

---

## M4 — Quality & Release

**Goal:** Library is publishable to npm with docs, tests, and CI all green  
**Target:** v0.1.0 on npm

### Features

**Test Coverage** - PLANNED

- Jest tests for JS layer (80%+ coverage on `src/`)
- Example app demonstrates resize + rotate with real camera output

**Documentation** - PLANNED

- README with installation, API reference, and usage examples
- Codegen spec comments as API docs

**Release Pipeline** - PLANNED

- Verify `release-it` + conventional changelog flow
- Publish v0.1.0 to npm

---

## Future Considerations

- Crop API (after rotate ships — shares decode/encode pipeline)
- Batch operations (resize + rotate in single native call)
- YUV↔RGB conversion as standalone API (camera frame use case)
- Expo plugin for managed workflow (currently out of scope)
- Performance benchmarks vs. alternative RN image libs
