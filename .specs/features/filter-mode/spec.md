# FilterMode Parameter Specification

## Problem Statement

`resize()` API hardcodes `kFilterBox` in Android JNI. Box filter is best for downscaling but suboptimal for upscaling or when speed matters over quality. Callers have no way to pick a different libyuv filter.

## Goals

- [ ] Expose libyuv's 4 filter modes via the JS API as a string union type
- [ ] Default to `'box'` — preserves existing behavior, no breaking change
- [ ] Android passes the chosen filter to `ARGBScale`
- [ ] iOS stub accepts and ignores the param (iOS uses vImage — out of scope for now)

## Out of Scope

- iOS vImage filter mapping (deferred; tracked in STATE.md)
- Exposing filter modes for rotation (only scaling uses FilterMode in libyuv)
- Runtime validation beyond enum check at JS boundary

---

## User Stories

### P1: Select filter mode at call site ⭐ MVP

**User Story**: As a React Native developer, I want to pass a `filterMode` option to `resize()` so that I can trade quality for speed or get better upscale quality.

**Why P1**: Core ask; single-file change in JS + Android.

**Acceptance Criteria**:

1. WHEN `filterMode` is omitted THEN system SHALL use `'box'` (no behavior change)
2. WHEN `filterMode: 'none'` THEN system SHALL call `ARGBScale` with `kFilterNone`
3. WHEN `filterMode: 'linear'` THEN system SHALL call `ARGBScale` with `kFilterLinear`
4. WHEN `filterMode: 'bilinear'` THEN system SHALL call `ARGBScale` with `kFilterBilinear`
5. WHEN `filterMode: 'box'` THEN system SHALL call `ARGBScale` with `kFilterBox`
6. WHEN an unknown string is passed THEN system SHALL throw a JS-side error before the native call

**Independent Test**: Call `resize()` with each `filterMode` value on a known image and confirm it completes without error; call with `filterMode: 'invalid'` and confirm rejection.

---

## Edge Cases

- WHEN `filterMode` is `undefined` or not provided THEN system SHALL default to `'box'`
- WHEN upscaling with `'none'` THEN system SHALL complete (pixelated result is valid)
- WHEN iOS receives any `filterMode` THEN system SHALL proceed without error (no-op mapping)

---

## API Shape

```typescript
type FilterMode = 'none' | 'linear' | 'bilinear' | 'box';

interface ResizeOptions {
  width: number;
  height: number;
  quality?: number;       // 0–100, default 80; 100 → PNG
  rotation?: number;      // 0, 90, 180, 270
  mode?: 'contain' | 'cover' | 'stretch';
  outputPath?: string;
  filterMode?: FilterMode; // NEW — default 'box'
}
```

The TurboModule bridge positional arg list adds `filterMode` as the last string param (after `outputPath`), consistent with existing sentinel pattern (`''` = absent/default).

---

## libyuv FilterMode Reference

| JS value    | libyuv constant   | Behavior |
|-------------|-------------------|----------|
| `'none'`    | `kFilterNone = 0` | Point/nearest — fastest |
| `'linear'`  | `kFilterLinear = 1` | Horizontal filter only |
| `'bilinear'`| `kFilterBilinear = 2` | Bilinear interpolation |
| `'box'`     | `kFilterBox = 3`  | Weighted average — best for downscale |

---

## Success Criteria

- [ ] Existing callers that omit `filterMode` see identical output (no regression)
- [ ] All 4 filter modes accepted without error on Android
- [ ] Invalid filter mode rejected at JS boundary with clear error message
- [ ] TypeScript types enforce the union — no `string` escape hatch
