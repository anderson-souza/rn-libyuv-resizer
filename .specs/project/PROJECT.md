# react-native-libyuv-resizer

**Vision:** A React Native Turbo Module that exposes libyuv's high-performance image resize and rotate operations to camera apps on iOS and Android.  
**For:** React Native developers building camera applications  
**Solves:** No ergonomic, performant native image manipulation library exists for RN New Architecture apps — existing solutions are slow (JS-side) or use deprecated bridge APIs.

## Goals

- Ship a typed, New Architecture-only API for resize and rotate with <5ms overhead vs raw libyuv on Android
- Zero-JS-thread blocking: all operations run on native thread, result returned via promise

## Tech Stack

**Core:**

- Framework: React Native 0.85+ (New Architecture / Turbo Modules only)
- Language: TypeScript (JS layer), Kotlin (Android module), Objective-C++ (iOS module), C++ (Android native via CMake)
- Native lib: libyuv (git submodule, `android.googlesource.com/platform/external/libyuv`)

**Key dependencies:**

- `react-native-builder-bob` — library build tooling
- CMake + Android NDK — C++ build for Android
- Apple Accelerate (`vImage`) — iOS image operations (no libyuv on iOS)

## Scope

**v1 includes:**

- Image resize (width × height, configurable scale mode)
- Image rotation (0°, 90°, 180°, 270°)
- Android: libyuv C++ implementation via JNI/JSI
- iOS: Accelerate framework implementation

**Explicitly out of scope:**

- Video stream / frame-by-frame pipeline
- Web / Expo managed workflow support
- YUV↔RGB color space conversion as a standalone API
- Crop, flip, or any other transform beyond resize + rotate

**In scope (added):**

- Legacy React Native bridge support alongside New Architecture (dual-compat via `TurboReactPackage` + `@ReactModule` + `RCT_EXPORT_METHOD`)
