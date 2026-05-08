# Custom Output Path — Tasks

**Design**: `.specs/features/custom-output-path/design.md`
**Status**: Approved

---

## Execution Plan

```
Phase 1 (Sequential — bridge contract first):
  T1 → T2

Phase 2 (Parallel — native impls independent):
  T2 complete, then:
    ├── T3 [P]  (Android)
    └── T4 [P]  (iOS)

Phase 3 (Sequential — tests + update STATE):
  T3 + T4 complete, then:
    T5 → T6
```

---

## Task Breakdown

### T1: Update `Spec` TurboModule bridge

**What**: Add `outputPath: string` as 7th positional param to `Spec.resize()` in `NativeLibyuvResizer.ts`
**Where**: `src/NativeLibyuvResizer.ts`
**Depends on**: None

**Done when**:
- [ ] `resize()` signature has `outputPath: string` as 7th param
- [ ] `yarn typecheck` passes with zero errors

**Verify**:
```bash
yarn typecheck
```

---

### T2: Update `ResizeOptions` + JS wrapper

**What**: Add `outputPath?: string` to `ResizeOptions`; extract it in `resize()` and pass to native (empty string when absent)
**Where**: `src/resizer.native.tsx`
**Depends on**: T1

**Done when**:
- [ ] `ResizeOptions` has `outputPath?: string`
- [ ] `resize()` passes `options?.outputPath ?? ''` as 7th arg to `LibyuvResizer.resize()`
- [ ] `src/resizer.tsx` (web fallback) ignores the new option gracefully — no change needed
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T3: Android — add `outputPath` param + `resolveOutputFile()` [P]

**What**: Add `outputPath: String` to `LibyuvResizerModule.resize()`; extract `resolveOutputFile()` helper; validate dir; replace inline `File(cacheDir, UUID)` call
**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
**Depends on**: T2 (codegen must reflect new param count)

**Implementation**:
```kotlin
private fun resolveOutputFile(inputFilePath: String, outputPath: String, ext: String): File {
    if (outputPath.isEmpty()) {
        return File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
    }
    val dir = File(outputPath)
    if (!dir.exists()) throw IllegalArgumentException("Output directory does not exist: $outputPath")
    if (!dir.isDirectory) throw IllegalArgumentException("outputPath must be a directory, not a file: $outputPath")
    return File(dir, File(inputFilePath).name)
}
```

Add `outputPath` validation block before bitmap decode (same position as other validations):
```kotlin
if (outputPath.isNotEmpty()) {
    val dir = File(outputPath)
    if (!dir.exists()) {
        promise.reject("E_INVALID_OUTPUT_PATH", "Output directory does not exist: $outputPath")
        return
    }
    if (!dir.isDirectory) {
        promise.reject("E_INVALID_OUTPUT_PATH", "outputPath must be a directory, not a file: $outputPath")
        return
    }
}
```

Replace line 95 (`File(cacheDir, UUID...)`) with:
```kotlin
val outFile = resolveOutputFile(filePath, outputPath, ext)
```

**Done when**:
- [ ] `resize()` accepts 7 params (codegen-generated `NativeLibyuvResizerSpec` updated)
- [ ] Empty `outputPath` → saves to `cacheDir/UUID.ext` (existing behavior)
- [ ] Valid dir `outputPath` → saves to `outputPath/<input-filename>`
- [ ] Non-existent dir → `promise.reject("E_INVALID_OUTPUT_PATH", ...)`
- [ ] `outputPath` is a file → `promise.reject("E_INVALID_OUTPUT_PATH", ...)`
- [ ] Android build passes: `yarn turbo run build:android`

**Verify**:
```bash
yarn turbo run build:android
```

---

### T4: iOS — add `outputPath` param to method signature [P]

**What**: Add `outputPath:(NSString *)outputPath` param to `resize` method in `LibyuvResizer.mm` and `LibyuvResizer.h`; stub validation (iOS resize not yet implemented — just match signature)
**Where**: `ios/LibyuvResizer.mm`, `ios/LibyuvResizer.h`
**Depends on**: T2

**Done when**:
- [ ] `resize` Obj-C method signature includes `outputPath:(NSString *)outputPath`
- [ ] iOS build passes: `yarn turbo run build:ios`

**Verify**:
```bash
yarn turbo run build:ios
```

---

### T5: Update unit tests

**What**: Update `src/__tests__/index.test.tsx` to cover `outputPath` option — both omitted and provided cases
**Where**: `src/__tests__/index.test.tsx`
**Depends on**: T3

**Done when**:
- [ ] Test: `resize()` without `outputPath` → native called with `''` as 7th arg
- [ ] Test: `resize()` with `outputPath: '/tmp/out'` → native called with `'/tmp/out'` as 7th arg
- [ ] `yarn test` passes

**Verify**:
```bash
yarn test
```

---

### T6: Update STATE.md

**What**: Mark feature complete in `.specs/project/STATE.md`; record `outputPath` param decision
**Where**: `.specs/project/STATE.md`
**Depends on**: T5

**Done when**:
- [ ] Active Work has `outputPath` feature checked
- [ ] Decisions table has row: `outputPath='' sentinel | TurboModule bridge requires fixed-arity positional args`

---

## Parallel Execution Map

```
T1 ──→ T2 ──┬──→ T3 [P] ──┬──→ T5 ──→ T6
             └──→ T4 [P] ──┘
```

---

## Granularity Check

| Task | Scope | Status |
|------|-------|--------|
| T1: Update TS bridge spec | 1 file, 1 param | ✅ |
| T2: Update JS wrapper + types | 1 file, 2 additions | ✅ |
| T3: Android native impl | 1 file, 1 helper + validation | ✅ |
| T4: iOS signature update | 2 files, param-only | ✅ |
| T5: Unit tests | 1 file, 2 test cases | ✅ |
| T6: STATE.md update | 1 file | ✅ |
