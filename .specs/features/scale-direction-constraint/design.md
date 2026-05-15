# Scale Direction Constraint — Design

## Overview

`scaleConstraint` is a new optional field on `ResizeOptions`. Constraint check happens in **two places**:

1. **JS layer** (`resizer.native.tsx`) — validates the value, then passes `scaleConstraint` as a plain string to the bridge
2. **Kotlin layer** (`LibyuvResizerModule.kt`) — reads raw src bitmap bounds (BitmapFactory inJustDecodeBounds), compares against raw `targetWidth`/`targetHeight`, short-circuits with `promise.resolve(filePath)` before any decode/resize work

Comparison uses **raw target dims vs raw decoded src dims** (no axis swap for rotation). Equal dims proceed to resize.

---

## Layer-by-layer changes

### 1. TypeScript — `src/resizer.native.tsx`

Add type and validation:

```typescript
export type ScaleConstraint = 'onlyScaleUp' | 'onlyScaleDown';

const VALID_SCALE_CONSTRAINTS: ScaleConstraint[] = ['onlyScaleUp', 'onlyScaleDown'];

export interface ResizeOptions {
  rotation?: RotationAngle;
  mode?: ResizeMode;
  outputPath?: string;
  filterMode?: FilterMode;
  scaleConstraint?: ScaleConstraint;  // new
}
```

In `resize()`, before calling the bridge:

```typescript
const scaleConstraint = options?.scaleConstraint ?? '';
if (scaleConstraint !== '' && !VALID_SCALE_CONSTRAINTS.includes(scaleConstraint as ScaleConstraint)) {
  return Promise.reject(new TypeError(`Invalid scaleConstraint: '${scaleConstraint}'`));
}
```

Pass `scaleConstraint` as the last arg to `LibyuvResizer.resize(...)`.

### 2. TypeScript — `src/NativeLibyuvResizer.ts`

Add `scaleConstraint: string` as last parameter to `resize()` spec:

```typescript
resize(
  filePath: string,
  targetWidth: number,
  targetHeight: number,
  quality: number,
  rotation: number,
  mode: string,
  outputPath: string,
  filterMode: string,
  scaleConstraint: string   // new
): Promise<string>;
```

### 3. TypeScript — `src/index.tsx`

Add `ScaleConstraint` to re-exports:

```typescript
export type { RotationAngle, ResizeMode, ResizeOptions, ScaleConstraint } from './resizer';
```

### 4. TypeScript — `src/resizer.tsx` (web stub)

Add `scaleConstraint` to `ResizeOptions` re-export — no behavior change (still rejects).

### 5. Kotlin — `ResizeParams.kt`

Add field:

```kotlin
data class ResizeParams(
  val filePath: String,
  val targetWidth: Int,
  val targetHeight: Int,
  val quality: Int,
  val rotation: Int,
  val mode: String,
  val outputPath: String,
  val filterMode: String,
  val scaleConstraint: String   // new; "" = unconstrained
)
```

### 6. Kotlin — `ResizeValidator.kt`

Add validation rule:

```kotlin
private val VALID_SCALE_CONSTRAINTS = setOf("", "onlyScaleUp", "onlyScaleDown")

// inside validate():
if (params.scaleConstraint !in VALID_SCALE_CONSTRAINTS)
  return ValidationResult.Invalid(
    "E_INVALID_SCALE_CONSTRAINT",
    "scaleConstraint must be onlyScaleUp or onlyScaleDown, got: ${params.scaleConstraint}"
  )
```

### 7. Kotlin — `LibyuvResizerModule.kt`

After `ResizeValidator.validate()` passes and `BitmapFactory.decodeFile(inJustDecodeBounds)` reads src dims, add the constraint check before full decode:

```kotlin
val srcW = boundsOpts.outWidth
val srcH = boundsOpts.outHeight

val skip = when (params.scaleConstraint) {
  "onlyScaleDown" -> targetW > srcW || targetH > srcH
  "onlyScaleUp"   -> targetW < srcW || targetH < srcH
  else            -> false
}
if (skip) {
  promise.resolve(filePath)
  return
}
```

This short-circuits before `BitmapFactory.decodeFile` (the expensive decode), so it's zero-cost.

### 8. iOS — `LibyuvResizer.mm`

Add `scaleConstraint:(NSString *)scaleConstraint` parameter to match the updated bridge spec. Keep the existing `reject(@"E_NOT_IMPLEMENTED", ...)` body — no behavior change needed.

---

## Data flow

```
resize(filePath, w, h, quality, options)
  └─ JS: validate scaleConstraint value
  └─ JS: call bridge with scaleConstraint string
       └─ Kotlin: ResizeValidator.validate() — checks string membership
       └─ Kotlin: BitmapFactory(inJustDecodeBounds) → srcW, srcH
       └─ Kotlin: constraint check → skip? → promise.resolve(filePath)
       └─ Kotlin: (normal path) full decode → computeDstDims → nativeResize → save
```

---

## Tests to add

### Unit — `ResizeValidatorTest.kt`

- `"onlyScaleDown"` and `"onlyScaleUp"` → `Valid`
- `""` → `Valid`
- `"both"` or any other string → `Invalid(E_INVALID_SCALE_CONSTRAINT)`

### Unit — `DimensionCalculatorTest.kt` (no change needed)

Constraint logic lives in `LibyuvResizerModule`, not `DimensionCalculator`.

### Instrumented — `LibyuvResizerModuleTest.kt`

| Test | src | target | constraint | expected |
|------|-----|--------|-----------|----------|
| onlyScaleDown skips upscale | 100×100 | 200×200 | onlyScaleDown | original path |
| onlyScaleDown skips when either dim upscales | 100×200 | 150×150 | onlyScaleDown | original path |
| onlyScaleDown resizes when both dims downscale | 400×300 | 200×150 | onlyScaleDown | new path |
| onlyScaleDown equal dims resizes | 200×200 | 200×200 | onlyScaleDown | new path |
| onlyScaleUp skips downscale | 400×300 | 200×150 | onlyScaleUp | original path |
| onlyScaleUp skips when either dim downscales | 400×200 | 300×300 | onlyScaleUp | original path |
| onlyScaleUp resizes when both dims upscale | 100×100 | 200×200 | onlyScaleUp | new path |
| onlyScaleUp equal dims resizes | 200×200 | 200×200 | onlyScaleUp | new path |
| no constraint | any | any | "" | new path |

### JS unit — `src/__tests__/index.test.ts` (or equivalent)

- Invalid `scaleConstraint` value → `TypeError`
- Valid values pass through without TypeError

---

## No-change areas

- `DimensionCalculator.kt` — untouched
- C++ native layer — untouched
- `LibyuvResizerPackage.kt` — untouched
- `resizer.tsx` (web stub) — only re-export type change

---

## Backwards compatibility

`scaleConstraint` is optional, defaults to `''` (unconstrained). All existing call sites pass `filterMode` as 8th arg; new `scaleConstraint` is 9th. Bridge is positional — existing callers that don't pass it will hit a compile error in TS (good: forces explicit opt-in) only if they call the native spec directly. Public `resize()` API handles the default transparently.
