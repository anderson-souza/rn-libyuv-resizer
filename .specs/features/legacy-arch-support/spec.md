# Legacy Architecture Support Specification

## Problem Statement

`react-native-libyuv-resizer` is wired exclusively to the New Architecture (Turbo Modules). Apps still on the legacy bridge (`JSC` + `NativeModules`) cannot load the module — `TurboModuleRegistry.getEnforcing` throws at startup, making the library a hard dependency on New Arch adoption. Teams that haven't migrated yet are blocked from using it.

## Goals

- [ ] Library loads and works on both New Architecture and Legacy Bridge without any consumer-side configuration.
- [ ] Zero-change integration for existing New Arch consumers.
- [ ] JS/TS API surface is identical on both architectures.

## Out of Scope

- iOS native implementation (remains `E_NOT_IMPLEMENTED` on both archs — tracked separately).
- Expo Go / managed workflow support.
- React Native < 0.68.

---

## User Stories

### P1: Legacy Android install does not crash ⭐ MVP

**User Story**: As a React Native developer on the legacy bridge, I want to install `react-native-libyuv-resizer` without crashing on app startup so that I can use image resizing without migrating to New Architecture first.

**Why P1**: Current behaviour is a hard crash at startup — library is completely unusable on legacy arch.

**Acceptance Criteria**:

1. WHEN an app with `newArchEnabled=false` installs the library THEN the module SHALL load without throwing.
2. WHEN `resize()` is called on a legacy-arch Android app THEN it SHALL resolve with the resized image path.
3. WHEN `resize()` is called on a new-arch Android app (unchanged) THEN it SHALL still resolve correctly.
4. WHEN the library is built in a legacy-arch project THEN `isTurboModule=false` SHALL be reflected in the package registration.

**Independent Test**: Build example app with `newArchEnabled=false`; call `resize()`; confirm it resolves without crash.

---

### P1: JS bridge compat layer ⭐ MVP

**User Story**: As a library consumer, I want the JS entry point to pick the correct native bridge automatically so that I don't need to configure anything.

**Why P1**: `TurboModuleRegistry.getEnforcing` is the only current JS path — it fails on legacy arch.

**Acceptance Criteria**:

1. WHEN `global.__turboModuleProxy` is non-null THEN the module SHALL use `TurboModuleRegistry.getEnforcing`.
2. WHEN `global.__turboModuleProxy` is null (legacy bridge) THEN the module SHALL use `NativeModules.LibyuvResizer`.
3. WHEN `NativeModules.LibyuvResizer` is null on legacy arch THEN a clear error SHALL be thrown (module not linked).
4. WHEN TypeScript consumers import `resize` THEN type signatures SHALL be identical on both archs.

**Independent Test**: Unit test mocking `global.__turboModuleProxy` as `null` — verify `NativeModules.LibyuvResizer` is called instead of `TurboModuleRegistry`.

---

### P2: Legacy iOS loads without crash

**User Story**: As a React Native developer on a legacy-bridge iOS app, I want the module to load without crashing so that the app starts successfully even though resize is not yet implemented.

**Why P2**: iOS `resize` is not implemented on either arch; parity between archs is the goal here, not full iOS implementation.

**Acceptance Criteria**:

1. WHEN an iOS legacy-arch app installs the library THEN the module SHALL load (RCT_EXPORT_MODULE registered).
2. WHEN `resize()` is called on legacy-arch iOS THEN it SHALL reject with `E_NOT_IMPLEMENTED` (same as New Arch today).

**Independent Test**: Build iOS example with legacy arch; call `resize()`; confirm `E_NOT_IMPLEMENTED` rejection (not a crash).

---

### P2: Build config propagation to library

**User Story**: As a library maintainer, I want the Android package registration to reflect the host app's architecture choice so that `ReactModuleInfo.isTurboModule` is accurate.

**Why P2**: Setting `isTurboModule = true` unconditionally on a legacy-arch host can cause bridge initialisation issues.

**Acceptance Criteria**:

1. WHEN host app has `newArchEnabled=true` THEN `ReactModuleInfo.isTurboModule` SHALL be `true`.
2. WHEN host app has `newArchEnabled=false` THEN `ReactModuleInfo.isTurboModule` SHALL be `false`.
3. WHEN neither property is set THEN it SHALL default to `false` (safe legacy default).

---

## Edge Cases

- WHEN `NativeModules.LibyuvResizer` is `undefined` on legacy arch THEN system SHALL throw `Error: react-native-libyuv-resizer: native module not found. Did you forget to link?`.
- WHEN `global.__turboModuleProxy` exists but module is not registered as a turbo module THEN `getEnforcing` SHALL throw (existing behaviour — app is misconfigured, not our concern).
- WHEN built with `newArchEnabled=true` and module spec changes THEN codegen SHALL still regenerate correctly (no regressions from compat changes).

---

## Success Criteria

- [ ] `yarn test` green with ≥ 85% coverage (existing threshold).
- [ ] Example app builds and `resize()` succeeds on Android with `newArchEnabled=false`.
- [ ] Example app builds and `resize()` succeeds on Android with `newArchEnabled=true` (no regression).
- [ ] No TypeScript errors (`yarn typecheck`).
- [ ] No lint errors (`yarn lint`).
