# FilterMode Parameter — Tasks

**Design**: `.specs/features/filter-mode/design.md`
**Status**: Approved

---

## Execution Plan

All 4 impl tasks depend on T1 (bridge spec). T2–T5 can then run in parallel. T6 (tests) depends on all impl tasks.

```
T1 ──┬──→ T2 [P]
     ├──→ T3 [P]  } parallel
     ├──→ T4 [P]
     └──→ T5 [P]
          └──────────────→ T6
```

---

## Task Breakdown

### T1: Add `filterMode` param to TurboModule spec

**What**: Add 8th positional `filterMode: string` param to `Spec.resize()` in the codegen bridge file.
**Where**: `src/NativeLibyuvResizer.ts`
**Depends on**: None

**Done when**:
- [ ] `resize()` signature has `filterMode: string` as 8th param
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T2: Add `FilterMode` type + validation to JS wrapper [P]

**What**: Export `FilterMode` union type, add `filterMode?: FilterMode` to `ResizeOptions`, validate before native call, default to `'box'`.
**Where**: `src/resizer.native.tsx`
**Depends on**: T1
**Reuses**: Existing `VALID_MODES` + `mode` validation pattern (lines 12, 28–30)

**Done when**:
- [ ] `FilterMode = 'none' | 'linear' | 'bilinear' | 'box'` exported
- [ ] `filterMode?: FilterMode` on `ResizeOptions`
- [ ] `VALID_FILTER_MODES` const defined
- [ ] Validation rejects unknown value with `TypeError("Invalid filter mode: '${mode}'")`
- [ ] `LibyuvResizer.resize(...)` call passes `filterMode` as 8th arg (defaults `'box'`)
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T3: Add `filterMode` param to Android Kotlin module [P]

**What**: Add `filterMode: String` as 8th param, validate it, map to int, pass to both JNI calls.
**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
**Depends on**: T1
**Reuses**: Existing `mode` validation pattern (line 48–51); existing `promise.reject` pattern

**Done when**:
- [ ] `override fun resize(...)` has `filterMode: String` as 8th param (before `promise`)
- [ ] Validation: `filterMode !in setOf("none", "linear", "bilinear", "box")` → reject `E_INVALID_FILTER_MODE`
- [ ] `private fun filterModeToInt(mode: String): Int` implemented (`none`→0, `linear`→1, `bilinear`→2, `box`→3)
- [ ] `nativeResize(srcBitmap, dstBitmap, filterModeToInt(filterMode))` updated
- [ ] `nativeResizeAndRotate(srcBitmap, dstBitmap, rot, filterModeToInt(filterMode))` updated

**Verify**: Build passes in T6.

---

### T4: Add `filterMode` param to JNI C++ functions [P]

**What**: Add `jint filterMode` param to both JNI functions; replace hardcoded `libyuv::kFilterBox` with `static_cast<libyuv::FilterModeEnum>(filterMode)`.
**Where**: `android/src/main/cpp/LibyuvResizerModule.cpp`
**Depends on**: T1
**Reuses**: Existing `ARGBScale` call structure (lines 45–51 and 99–105)

**Done when**:
- [ ] `Java_com_libyuvresizer_LibyuvResizerModule_nativeResize` has `jint filterMode` param
- [ ] `Java_com_libyuvresizer_LibyuvResizerModule_nativeResizeAndRotate` has `jint filterMode` param
- [ ] Both `ARGBScale` calls use `static_cast<libyuv::FilterModeEnum>(filterMode)` instead of `libyuv::kFilterBox`
- [ ] No other changes to the file

**Verify**: Build passes in T6.

---

### T5: Update iOS stub to match new bridge signature [P]

**What**: Add `filterMode` param to iOS `resize` method signature to match updated codegen spec; no functional change.
**Where**: `ios/LibyuvResizer.mm` (and `ios/LibyuvResizer.h` if needed)
**Depends on**: T1

**Done when**:
- [ ] iOS method signature accepts `filterMode` param
- [ ] Param silently ignored (iOS not yet implemented)
- [ ] No compilation errors

**Verify**: Build passes in T6.

---

### T6: Build + smoke test all filter modes

**What**: Verify Android build succeeds and all 4 filter modes work end-to-end in the example app.
**Where**: `example/` + Android build
**Depends on**: T2, T3, T4, T5

**Done when**:
- [ ] `yarn typecheck` passes (no TS errors)
- [ ] Android example app builds: `yarn turbo run build:android`
- [ ] `resize()` with no `filterMode` succeeds (default `'box'` behavior unchanged)
- [ ] `resize()` with `filterMode: 'none'` succeeds
- [ ] `resize()` with `filterMode: 'linear'` succeeds
- [ ] `resize()` with `filterMode: 'bilinear'` succeeds
- [ ] `resize()` with `filterMode: 'box'` succeeds
- [ ] `resize()` with `filterMode: 'invalid'` rejects with `TypeError`

**Verify**:
```bash
yarn typecheck
yarn turbo run build:android
```

---

## Parallel Execution Map

```
Phase 1 (Sequential):
  T1: TurboModule spec

Phase 2 (Parallel — all depend on T1):
  ├── T2: JS wrapper [P]
  ├── T3: Kotlin module [P]
  ├── T4: C++ JNI [P]
  └── T5: iOS stub [P]

Phase 3 (Sequential):
  T6: Build + smoke test
```
