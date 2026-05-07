# libyuv Android Integration Specification

## Problem Statement

The library scaffolding is in place (Turbo Module spec, Kotlin module, CMakeLists.txt, libyuv submodule) but no actual image resizing functionality is wired end-to-end. The JS API exposes only a placeholder `multiply` method. Android has no C++ implementation file and no JNI bridge — libyuv is referenced in CMake but never called.

## Goals

- [ ] Expose a `resize(filePath, targetWidth, targetHeight, quality)` API from JS/TS through to native on Android
- [ ] Android implementation calls libyuv `I420Scale` (or `ARGBScale`) via a C++ JNI layer
- [ ] Build compiles cleanly for all four ABI targets (armeabi-v7a, x86, x86_64, arm64-v8a)
- [ ] Round-trip verified: JS call → Kotlin → JNI → libyuv → resized output returned to JS

## Out of Scope

- iOS implementation (uses Apple vImage/Accelerate — separate feature)
- JPEG/PNG encode/decode in C++ (use Android's `BitmapFactory` on the Kotlin side)
- Rotation, cropping, colour-space conversion beyond what resize requires
- Web/JS fallback implementation

---

## User Stories

### P1: JS can call resize and get a result path ⭐ MVP

**User Story**: As a React Native developer, I want to call `LibyuvResizer.resize(path, w, h, quality)` so that I get back the file path of a resized image.

**Why P1**: Core deliverable — nothing else works without this vertical slice.

**Acceptance Criteria**:

1. WHEN `NativeLibyuvResizer.ts` is updated THEN it SHALL export `resize(filePath: string, targetWidth: number, targetHeight: number, quality: number): Promise<string>`
2. WHEN `LibyuvResizerModule.kt` receives a `resize` call THEN it SHALL decode the source bitmap, pass raw pixel data to the C++ layer, and return the output file path via a Promise
3. WHEN the C++ layer (`LibyuvResizerModule.cpp`) is called THEN it SHALL invoke `libyuv::ARGBScale` (or `I420Scale`) and write the result back to a temp file
4. WHEN the Android build runs THEN it SHALL compile without errors for all four ABIs

**Independent Test**: Install example app on Android emulator, call `resize` on a test PNG, verify output file exists and has expected dimensions.

---

### P1: CMake + libyuv build is green ⭐ MVP

**User Story**: As a library maintainer, I want the CMake configuration to compile libyuv and link it into the shared library so that CI stays green.

**Why P1**: Prerequisite for any C++ work — nothing ships broken.

**Acceptance Criteria**:

1. WHEN `CMakeLists.txt` is processed THEN it SHALL include `add_subdirectory(../libyuv libyuv)` without errors
2. WHEN `target_link_libraries` runs THEN it SHALL link the `yuv` static target and `android`, `log`, `jnigraphics` system libs
3. WHEN `yarn turbo run build:android` runs THEN it SHALL exit 0 with no CMake or linker errors
4. WHEN any of the four ABI targets is built THEN the resulting `.so` SHALL export the JNI symbol for `resize`

**Independent Test**: Run `cd example && ./gradlew :rn-libyuv-resizer:assembleDebug` — zero errors.

---

### P2: Error handling and input validation

**User Story**: As a React Native developer, I want meaningful errors when resize fails so that I can handle edge cases in my app.

**Why P2**: Needed for production use but doesn't block MVP demo.

**Acceptance Criteria**:

1. WHEN `filePath` does not exist THEN the Promise SHALL reject with `"File not found: <path>"`
2. WHEN `targetWidth` or `targetHeight` is ≤ 0 THEN the Promise SHALL reject with `"Invalid dimensions"`
3. WHEN `quality` is outside `[1, 100]` THEN the Promise SHALL reject with `"Quality must be between 1 and 100"`
4. WHEN native throws an uncaught exception THEN Kotlin SHALL catch it and reject the Promise with the exception message

**Independent Test**: Call `resize` with a non-existent path, verify Promise rejects with expected message.

---

### P3: Performance — large image stays under 500ms

**User Story**: As a React Native developer, I want resizing a 4K image to complete in under 500ms so that the UI stays responsive.

**Why P3**: libyuv is fast by design; this is a validation, not blocking MVP.

**Acceptance Criteria**:

1. WHEN a 3840×2160 ARGB image is resized to 1920×1080 on an arm64-v8a device THEN the C++ layer SHALL complete in under 200ms
2. WHEN measured end-to-end from JS call to Promise resolve THEN total time SHALL be under 500ms

**Independent Test**: Add a benchmark test in the example app that logs timings.

---

## Edge Cases

- WHEN source image is already smaller than target THEN system SHALL upscale (libyuv supports this)
- WHEN source and target dimensions are identical THEN system SHALL return output without calling libyuv scale
- WHEN available disk space is insufficient to write output THEN system SHALL reject the Promise with a storage error
- WHEN `quality` is 100 THEN output SHALL be lossless (PNG) or highest-quality JPEG
- WHEN called concurrently from multiple JS threads THEN each call SHALL be independent with no shared mutable state

---

## Success Criteria

- [ ] `yarn turbo run build:android` exits 0 with libyuv linked
- [ ] Example app can resize a real image and display the result on Android
- [ ] All four ABI builds produce a valid `.so` that exports the JNI `resize` symbol
- [ ] P2 error cases all produce typed Promise rejections
