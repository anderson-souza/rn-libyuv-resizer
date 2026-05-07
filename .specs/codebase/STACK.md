# Tech Stack

**Analyzed:** 2026-05-06

## Core

- Framework: React Native 0.85 (New Architecture / Turbo Modules only)
- Language (JS/TS): TypeScript 6.x
- Language (Android): Kotlin 2.0.21
- Language (iOS): Objective-C++ (`.mm`)
- Language (native C++): C++17 — not yet implemented, planned via Android NDK/CMake
- Package manager: Yarn 4.11.0 (Berry, PnP-less with node_modules)

## Native Libraries

- **libyuv** — Google's YUV image conversion/scaling C++ library (planned git submodule, Android only)
- **Apple Accelerate / vImage** — iOS image operations (system framework, no install needed)

## Build Tooling

- `react-native-builder-bob` 0.41 — compiles `src/` to `lib/` (ESM + TypeScript declarations)
- `turbo` 2.x — task runner for monorepo (`build:android`, `build:ios`)
- CMake 3.9+ — Android native build (not yet configured)
- Android NDK — C++ compilation for Android (not yet configured)

## Testing

- Unit: Jest 30 with `@react-native/jest-preset`
- Coverage: `--coverage` flag (no target enforced yet)
- E2E: none

## Development Tools

- Linter: ESLint 9 (flat config) + `@react-native/eslint-config` + `eslint-plugin-prettier`
- Formatter: Prettier 3
- Commit lint: commitlint 20 + `@commitlint/config-conventional`
- Git hooks: lefthook 2
- Release: release-it 19 + `@release-it/conventional-changelog`
- Codegen: React Native codegen (`codegenConfig` in `package.json`) — generates `LibyuvResizerSpec`
