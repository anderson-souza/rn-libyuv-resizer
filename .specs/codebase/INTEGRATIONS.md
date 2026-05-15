# External Integrations

## Native Libraries

### libyuv (Android — active)

**Service:** Google libyuv — `android.googlesource.com/platform/external/libyuv`  
**Purpose:** High-performance YUV/ARGB image scaling and rotation via SIMD (NEON, SVE2)  
**Implementation:** Git submodule at `libyuv/` (root); `android/CMakeLists.txt` uses `add_subdirectory` + links `yuv` static target; JNI calls in `android/src/main/cpp/LibyuvResizerModule.cpp`  
**Configuration:** Pinned by git commit SHA in `.gitmodules` — no version tags  
**Status:** Implemented; `nativeResize` (ARGBScale) and `nativeResizeAndRotate` (ARGBScale + ARGBRotate) active on Android

### Apple Accelerate / vImage (iOS — planned)

**Service:** System framework — no install, no submodule  
**Purpose:** Replace libyuv on iOS for resize and rotation (libyuv has no iOS distribution)  
**Implementation:** Link via podspec system frameworks; call `vImageScale_ARGB8888`  
**Status:** Stub only — iOS build job disabled (`if: false`) in CI until implemented

## CI/CD

### GitHub Actions

**Workflows:** `.github/workflows/ci.yml`  
**Jobs:**

| Job | Runner | Purpose | Needs |
|-----|--------|---------|-------|
| `lint` | ubuntu-latest | ESLint + TypeScript typecheck | — |
| `test` | ubuntu-latest | Jest unit tests + coverage upload | — |
| `test-android` | ubuntu-latest | Android JVM unit tests via Gradle | — |
| `build-library` | ubuntu-latest | `yarn prepare` (bob build) | lint, test |
| `build-android` | ubuntu-latest | Gradle build of example app | lint, test, test-android |
| `build-ios` | macos-latest | Xcode build — **disabled** (`if: false`) | lint, test |

**Coverage upload:** `actions/upload-artifact` on `test` job → `coverage/` dir, 7-day retention  
**Caching:** Turborepo (`.turbo/android`, `.turbo/ios`) + Gradle (`~/.gradle/wrapper`, `~/.gradle/caches`) + CocoaPods (`example/ios/Pods`)  
**Submodules:** `checkout` with `submodules: true` on `build-android` to pull libyuv  
**Trigger:** push/PR to `main`, merge group

### npm Registry

**Service:** `registry.npmjs.org`  
**Purpose:** Package publishing  
**Config:** `publishConfig.registry` in `package.json`  
**Tool:** `release-it` + `@release-it/conventional-changelog` — auto-generates CHANGELOG, bumps version, tags, creates GitHub release, publishes to npm

## No Other External Integrations

Pure native library — no backend APIs, databases, authentication, webhooks, analytics, or crash reporting.
