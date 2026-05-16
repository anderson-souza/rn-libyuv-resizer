# Legacy Architecture Support Tasks

**Design**: `.specs/features/legacy-arch-support/design.md`
**Status**: Draft

---

## Execution Plan

```
Phase 1 (Sequential — foundation):
  T1 → T2 → T3

Phase 2 (Parallel — platform impls):
  T3 complete, then:
    ├── T4 [P]   Android: build.gradle buildConfigField
    ├── T5 [P]   Android: @ReactModule annotation
    ├── T6 [P]   iOS: header protocol conformance
    └── T7 [P]   Update unit tests

Phase 3 (Sequential — wiring):
  T4 + T5 complete → T8   Android: package swap
  T6 complete        → T9  iOS: RCT_EXPORT_METHOD

Phase 4 (Sequential — validation):
  All complete → T10  Typecheck + lint + tests
```

---

## Task Breakdown

### T1: Add `global.__turboModuleProxy` compat guard to `NativeLibyuvResizer.ts`

**What**: Replace `TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer')` (last line) with a runtime arch-detection guard that falls back to `NativeModules.LibyuvResizer` on legacy bridge.

**Where**: `src/NativeLibyuvResizer.ts`

**Depends on**: None

**Reuses**: Existing `Spec` interface — no change to interface.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `NativeModules` imported from `react-native`.
- [ ] `global.__turboModuleProxy != null` used as arch detection.
- [ ] New Arch path: `TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer')`.
- [ ] Legacy path: `NativeModules.LibyuvResizer as Spec`.
- [ ] `yarn typecheck` passes (no TypeScript errors).

**Verify**:
```bash
yarn typecheck
```

---

### T2: Add null-guard with clear error message for legacy path

**What**: Wrap the legacy `NativeModules.LibyuvResizer` reference so that if the module is not linked, a helpful error is thrown immediately (not a cryptic `null.resize is not a function`).

**Where**: `src/NativeLibyuvResizer.ts`

**Depends on**: T1

**Reuses**: Same file as T1.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] If `NativeModules.LibyuvResizer` is falsy, an `Error` is thrown with message: `"react-native-libyuv-resizer: native module not found. Did you forget to link the library?"`.
- [ ] Error only thrown on legacy path (no change to turbo path).
- [ ] `yarn typecheck` passes.

**Verify**:
```bash
yarn typecheck
```

---

### T3: Update unit tests for arch-detection logic

**What**: Add tests that mock `global.__turboModuleProxy` (null and non-null) and verify the correct bridge path is selected.

**Where**: `src/__tests__/index.test.tsx`

**Depends on**: T1, T2

**Reuses**: Existing Jest test file and mocking patterns.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Test: when `global.__turboModuleProxy = {}` → `TurboModuleRegistry.getEnforcing` is called.
- [ ] Test: when `global.__turboModuleProxy = null` → `NativeModules.LibyuvResizer` is used.
- [ ] Test: when `global.__turboModuleProxy = null` AND `NativeModules.LibyuvResizer = undefined` → error thrown with expected message.
- [ ] `yarn test` green with ≥ 85% coverage maintained.

**Verify**:
```bash
yarn test --testPathPattern="src/__tests__/index"
```

---

### T4: Add `IS_NEW_ARCHITECTURE_ENABLED` buildConfigField to `android/build.gradle` [P]

**What**: Inject `IS_NEW_ARCHITECTURE_ENABLED` boolean into the library's `BuildConfig` by reading the host app's `newArchEnabled` Gradle property.

**Where**: `android/build.gradle`

**Depends on**: T3 (can start after T3, independent of T5/T6/T7)

**Reuses**: Standard RN Gradle convention (`project.hasProperty("newArchEnabled")`).

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `buildConfigField("boolean", "IS_NEW_ARCHITECTURE_ENABLED", ...)` added inside `android { defaultConfig { ... } }`.
- [ ] Defaults to `"false"` when property is absent.
- [ ] Reads `project.getProperty("newArchEnabled")` when property present.
- [ ] `./gradlew :react-native-libyuv-resizer:assembleDebug` compiles without error (run from `example/android`).

**Verify**:
```bash
cd example/android && ./gradlew :react-native-libyuv-resizer:assembleDebug
```

---

### T5: Add `@ReactModule` annotation to `LibyuvResizerModule.kt` [P]

**What**: Annotate `LibyuvResizerModule` with `@ReactModule(name = LibyuvResizerModule.NAME)` so the legacy bridge's module registry can discover it via reflection.

**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`

**Depends on**: T3

**Reuses**: Existing `NAME` companion constant.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `@ReactModule(name = LibyuvResizerModule.NAME)` annotation on the class declaration.
- [ ] Import `com.facebook.react.module.annotations.ReactModule` added.
- [ ] No other logic changes.
- [ ] File compiles (verified via Gradle build in T8).

**Verify**: Confirmed when T8 Gradle build passes.

---

### T6: Add `RCTBridgeModule` conformance to `ios/LibyuvResizer.h` [P]

**What**: Add `<RCTBridgeModule>` protocol to the `LibyuvResizer` interface declaration so the legacy iOS bridge recognises it as a native module.

**Where**: `ios/LibyuvResizer.h`

**Depends on**: T3

**Reuses**: Existing header.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `@interface LibyuvResizer : NSObject <NativeLibyuvResizerSpec, RCTBridgeModule>` (both protocols listed).
- [ ] `#import <React/RCTBridgeModule.h>` added if not already transitively included.

**Verify**: Confirmed when T9 iOS pod build passes.

---

### T7: Update `android/build.gradle` import in `LibyuvResizerPackage.kt` for `TurboReactPackage` [P]

**What**: Identify the correct import for `TurboReactPackage` in the target RN version (0.85) and confirm it's available in the existing Gradle dependency graph. No code change needed if already on classpath.

**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerPackage.kt` (import line only)

**Depends on**: T3

**Reuses**: Existing package file.

**Tools**:
- MCP: context7 (if `TurboReactPackage` import path needs verification)
- Skill: NONE

**Done when**:
- [ ] Confirmed `com.facebook.react.TurboReactPackage` is available in RN 0.85 dependency tree.
- [ ] Import ready for T8 (no compilation errors when used).

**Verify**: Resolved prior to T8.

---

### T8: Swap `BaseReactPackage` → `TurboReactPackage` in `LibyuvResizerPackage.kt`

**What**: Change the superclass from `BaseReactPackage` to `TurboReactPackage` and set `isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED`.

**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerPackage.kt`

**Depends on**: T4, T5, T7

**Reuses**: Existing `getModule` + `getReactModuleInfoProvider` bodies verbatim — only superclass and `isTurboModule` value change.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `class LibyuvResizerPackage : TurboReactPackage()` (not `BaseReactPackage`).
- [ ] `isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED` in `ReactModuleInfo`.
- [ ] Import updated: `com.facebook.react.TurboReactPackage`.
- [ ] Old `BaseReactPackage` import removed.
- [ ] `./gradlew :react-native-libyuv-resizer:assembleDebug` passes (from `example/android`).

**Verify**:
```bash
cd example/android && ./gradlew :react-native-libyuv-resizer:assembleDebug
```

---

### T9: Add `RCT_EXPORT_MODULE` + `RCT_EXPORT_METHOD` to `ios/LibyuvResizer.mm`

**What**: Register the module on the legacy iOS bridge by adding `RCT_EXPORT_MODULE(LibyuvResizer)` and a `RCT_EXPORT_METHOD` that mirrors the existing turbo `resize` signature, rejecting with `E_NOT_IMPLEMENTED`.

**Where**: `ios/LibyuvResizer.mm`

**Depends on**: T6

**Reuses**: Existing `reject(@"E_NOT_IMPLEMENTED", ...)` body from the current `resize:` implementation.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `RCT_EXPORT_MODULE(LibyuvResizer)` added (before `@end`).
- [ ] `RCT_EXPORT_METHOD(resize:targetWidth:targetHeight:quality:rotation:mode:outputPath:filterMode:resolve:reject:)` macro added with same parameter names as turbo impl.
- [ ] Method body rejects with `E_NOT_IMPLEMENTED`.
- [ ] Existing `getTurboModule:` and `+ moduleName` methods unchanged.
- [ ] Pod build passes (mental check; CI will confirm).

**Verify**: iOS pod build in CI / local `pod install && xcodebuild`.

---

### T10: Full validation pass

**What**: Run all quality gates to confirm no regressions.

**Where**: Repo root

**Depends on**: T8, T9

**Reuses**: Existing CI scripts.

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `yarn typecheck` — zero errors.
- [ ] `yarn lint` — zero errors.
- [ ] `yarn test` — all tests green, ≥ 85% coverage.

**Verify**:
```bash
yarn typecheck && yarn lint && yarn test
```

---

## Parallel Execution Map

```
Phase 1 (Sequential):
  T1 ──→ T2 ──→ T3

Phase 2 (Parallel — after T3):
  T3 ──┬── T4 [P]
       ├── T5 [P]
       ├── T6 [P]
       └── T7 [P]

Phase 3 (Sequential chains):
  T4 + T5 + T7 ──→ T8
  T6           ──→ T9

Phase 4 (Sequential):
  T8 + T9 ──→ T10
```

---

## Task Granularity Check

| Task | Scope | Status |
|---|---|---|
| T1: compat guard | 1 file, last 3 lines | ✅ Granular |
| T2: null-guard error | 1 file, ~5 lines | ✅ Granular |
| T3: unit tests | 1 test file | ✅ Granular |
| T4: buildConfigField | 1 file, 3 lines | ✅ Granular |
| T5: @ReactModule annotation | 1 annotation, 1 import | ✅ Granular |
| T6: iOS header protocol | 1 file, 1 line change | ✅ Granular |
| T7: TurboReactPackage verification | Research only | ✅ Granular |
| T8: package superclass swap | 1 file, ~4 line changes | ✅ Granular |
| T9: RCT_EXPORT macros | 1 file, ~15 lines added | ✅ Granular |
| T10: validation | Commands only | ✅ Granular |
