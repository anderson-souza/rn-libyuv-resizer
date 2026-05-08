# Resize Mode Tasks

**Design**: `.specs/features/resize-mode/design.md`
**Status**: Approved

---

## Execution Plan

```
Phase 1 — Rename scaffold (sequential):
  T1 → T2 → T3

Phase 2 — Types (unblocks all downstream):
  T3 complete, then:
    T4

Phase 3 — Implementation (parallel after T4):
  T4 complete, then:
    ├── T5 [P]  Kotlin mode math
    └── T6 [P]  JS forward mode to native

Phase 4 — Finalize (after T5 + T6):
  T5, T6 complete, then:
    ├── T7 [P]  unit tests
    └── T8 [P]  export ResizeMode from index
```

---

## Task Breakdown

### T1: Rename `multiply.native.tsx` → `resizer.native.tsx`

**What**: Rename file only; content unchanged.
**Where**: `src/resizer.native.tsx`
**Depends on**: None

**Done when**:
- [ ] `src/multiply.native.tsx` deleted
- [ ] `src/resizer.native.tsx` exists with identical content

**Verify**: `Get-ChildItem src/` shows `resizer.native.tsx`, no `multiply.native.tsx`

---

### T2: Rename `multiply.tsx` → `resizer.tsx` and fix its import

**What**: Rename file; update internal import from `'./multiply.native'` → `'./resizer.native'`.
**Where**: `src/resizer.tsx`
**Depends on**: T1

**Done when**:
- [ ] `src/multiply.tsx` deleted
- [ ] `src/resizer.tsx` exists
- [ ] Single import line reads `from './resizer.native'`

**Verify**: `Get-ChildItem src/` shows `resizer.tsx`, no `multiply.tsx`

---

### T3: Update `index.tsx` imports

**What**: Change both import lines from `'./multiply'` → `'./resizer'`.
**Where**: `src/index.tsx`
**Depends on**: T2

**Done when**:
- [ ] Zero references to `multiply` in `src/index.tsx`
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T4: Add `ResizeMode` type and update `ResizeOptions`

**What**: Define `ResizeMode` string literal union; add `mode` field to `ResizeOptions`; export both.
**Where**: `src/resizer.native.tsx`
**Depends on**: T3

**Done when**:
- [ ] `export type ResizeMode = 'contain' | 'cover' | 'stretch'` present
- [ ] `ResizeOptions` has `mode?: ResizeMode`
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T5: Add mode scale math to `LibyuvResizerModule.kt` [P]

**What**: After `BitmapFactory.decodeFile`, read `mode` param, compute actual `dstW`/`dstH`, then `Bitmap.createBitmap` with computed dims.
**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
**Depends on**: T4

**Logic** (insert after `val srcBitmap = ...` block, before `val dstBitmap = ...`):
```kotlin
val validModes = setOf("contain", "cover", "stretch")
if (mode !in validModes) {
    promise.reject("E_INVALID_MODE", "mode must be contain, cover, or stretch, got: $mode")
    return
}
val srcW = srcBitmap.width.toDouble()
val srcH = srcBitmap.height.toDouble()
val (dstW, dstH) = when (mode) {
    "stretch" -> Pair(targetWidth.toInt(), targetHeight.toInt())
    else -> {
        val scale = if (mode == "contain")
            minOf(targetWidth / srcW, targetHeight / srcH)
        else
            maxOf(targetWidth / srcW, targetHeight / srcH)
        Pair(
            maxOf(1, (srcW * scale).roundToInt()),
            maxOf(1, (srcH * scale).roundToInt())
        )
    }
}
```

Also: add `mode: String` as the 6th parameter to `override fun resize(...)`.

**Done when**:
- [ ] `mode: String` param added to `override fun resize`
- [ ] Mode validation block present with `E_INVALID_MODE` reject
- [ ] `dstW`/`dstH` derived from mode math (not raw `targetWidth`/`targetHeight`)
- [ ] `kotlin.math.roundToInt` imported
- [ ] Project compiles: `./gradlew :android:compileDebugKotlin`

**Verify**:
```bash
yarn turbo run build:android
```

---

### T6: Forward `mode` through JS bridge [P]

**What**: Pass `mode` string from `ResizeOptions` to native; add JS-side `TypeError` for invalid values; update `NativeLibyuvResizer.ts` signature.
**Where**: `src/resizer.native.tsx`, `src/NativeLibyuvResizer.ts`
**Depends on**: T4

**Changes**:

`src/NativeLibyuvResizer.ts` — add `mode` param:
```typescript
resize(
  filePath: string,
  targetWidth: number,
  targetHeight: number,
  quality: number,
  rotation: number,
  mode: string
): Promise<string>;
```

`src/resizer.native.tsx` — validate and forward:
```typescript
const VALID_MODES: ResizeMode[] = ['contain', 'cover', 'stretch'];
const mode = options?.mode ?? 'contain';
if (!VALID_MODES.includes(mode)) {
  return Promise.reject(new TypeError(`Invalid resize mode: '${mode}'`));
}
return LibyuvResizer.resize(filePath, targetWidth, targetHeight, quality, rotation, mode);
```

**Done when**:
- [ ] `NativeLibyuvResizer.ts` has 6-param `resize` signature
- [ ] `resizer.native.tsx` validates mode and rejects with `TypeError` for invalid values
- [ ] Default mode is `'contain'`
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T7: Write unit tests for mode logic [P]

**What**: Unit tests for all three modes and edge cases. Test via JS layer (mock native call, assert args passed).
**Where**: `src/__tests__/resizer.test.ts` (new or extend existing)
**Depends on**: T5, T6

**Test cases** (minimum):
- `contain`, landscape (1920×1080), square target (400×400) → native called with `dstW=400, dstH=225`
- `contain`, portrait (1080×1920), square target (400×400) → native called with `dstW=225, dstH=400`
- `cover`, landscape (1920×1080), square target (400×400) → native called with `dstW=711, dstH=400`
- `cover`, portrait (1080×1920), square target (400×400) → native called with `dstW=400, dstH=711`
- `stretch`, any image, (400×400) → native called with `dstW=400, dstH=400`
- mode omitted → behaves as `contain`
- invalid mode string → `TypeError` thrown, native NOT called

**Done when**:
- [ ] All cases listed above have passing tests
- [ ] `yarn test` exits 0

**Verify**:
```bash
yarn test
```

---

### T8: Export `ResizeMode` from `index.tsx` [P]

**What**: Add `ResizeMode` to public exports.
**Where**: `src/index.tsx`
**Depends on**: T6

**Done when**:
- [ ] `export type { RotationAngle, ResizeOptions, ResizeMode } from './resizer'`
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

## Parallel Execution Map

```
T1 → T2 → T3 → T4 ─┬─→ T5 [P] ─┬─→ T7 [P]
                    └─→ T6 [P] ─┘
                                  └─→ T8 [P]
```
