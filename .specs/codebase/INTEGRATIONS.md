# External Integrations

## Native Libraries

### libyuv (Android, planned)

**Service:** Google libyuv — `android.googlesource.com/platform/external/libyuv`  
**Purpose:** High-performance YUV image conversion, scaling, and rotation via SIMD (NEON, SVE2)  
**Implementation:** Git submodule at `libyuv/` (root); integrated via `add_subdirectory` in `android/CMakeLists.txt`; linked as the `yuv` static target  
**Configuration:** No version tags — pinned by git commit SHA in `.gitmodules`  
**Status:** Not yet added; integration steps documented in CLAUDE.md

### Apple Accelerate / vImage (iOS, planned)

**Service:** System framework — no install, no submodule  
**Purpose:** Replace libyuv on iOS for resize and rotation (libyuv has no iOS distribution)  
**Implementation:** Link via podspec or Xcode system frameworks; call `vImageScale_ARGB8888` / `vImageRotate90_ARGB8888`  
**Status:** Not yet implemented

## CI/CD

### GitHub Actions

**Workflows:** `.github/workflows/ci.yml`  
**Jobs:**
| Job | Runner | Purpose |
|-----|--------|---------|
| `lint` | ubuntu-latest | ESLint + TypeScript typecheck |
| `test` | ubuntu-latest | Jest unit tests with coverage |
| `build-library` | ubuntu-latest | `yarn prepare` (bob build) |
| `build-android` | ubuntu-latest | Gradle build of example app |
| `build-ios` | macos-latest | Xcode build of example app (Xcode 26) |

**Caching:** Turborepo cache (`.turbo/android`, `.turbo/ios`) + Gradle cache (`~/.gradle`)  
**Trigger:** push/PR to `main`, merge group

### npm Registry

**Service:** `registry.npmjs.org`  
**Purpose:** Package publishing  
**Config:** `publishConfig.registry` in `package.json`  
**Tool:** `release-it` + `@release-it/conventional-changelog` — auto-generates CHANGELOG, bumps version, tags, creates GitHub release, publishes to npm

## No Other External Integrations

This is a pure native library — no backend APIs, no databases, no authentication services, no webhooks, no analytics, no crash reporting.
