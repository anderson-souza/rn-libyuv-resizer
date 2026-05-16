# Tech Stack

**Analyzed:** 2026-05-14

## Core

- Framework: React Native 0.85 (New Architecture + Legacy Bridge dual support)
- Language (JS/TS): TypeScript 6.x
- Language (Android): Kotlin 2.0.21
- Language (iOS): Objective-C++ (`.mm`)
- Language (native C++): C++17 — implemented via Android NDK/CMake (`android/src/main/cpp/`)
- Package manager: Yarn 4.11.0 (Berry, PnP-less with node_modules)

## Native Libraries

- **libyuv** — Google's YUV image conversion/scaling C++ library (git submodule at `libyuv/`, Android only)
- **Apple Accelerate / vImage** — iOS image operations (system framework, planned — not yet implemented)

## Build Tooling

- `react-native-builder-bob` 0.41 — compiles `src/` to `lib/` (ESM + TypeScript declarations)
- `turbo` 2.x — task runner for monorepo (`build:android`, `build:ios`)
- CMake 3.9+ — Android native build (configured in `android/CMakeLists.txt`)
- Android NDK — C++ compilation for Android (active; `externalNativeBuild` in `android/build.gradle`)

## Testing

- Unit (JS): Jest 30 with `@react-native/jest-preset`; 85% coverage threshold enforced
- Unit (Android JVM): Gradle test task via `android/src/test/` (JUnit via Android test infrastructure)
- Integration (Android): Instrumented tests in `android/src/androidTest/`
- Coverage upload: CI artifact (`coverage/` directory, 7-day retention)
- E2E: none

## Development Tools

- Linter: ESLint 9 (flat config) + `@react-native/eslint-config` + `eslint-plugin-prettier`
- Formatter: Prettier 3
- Commit lint: commitlint 20 + `@commitlint/config-conventional`
- Git hooks: lefthook 2
- Release: release-it 19 + `@release-it/conventional-changelog`
- Codegen: React Native codegen (`codegenConfig` in `package.json`) — generates `LibyuvResizerSpec`
