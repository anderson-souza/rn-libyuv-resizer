# Scale Direction Constraint — Specification

## Problem Statement

Callers sometimes want to prevent image enlargement (e.g., avoid blurry upscaled thumbnails) or prevent shrinking (e.g., keep high-res originals for print). Today `resize()` always scales toward the requested dimensions with no guard. Adding a `scaleConstraint` option gives callers fine-grained control with a single, mutually-exclusive enum.

## Goals

- [ ] Add `scaleConstraint?: ScaleConstraint` to `ResizeOptions` (TS + native bridge)
- [ ] `onlyScaleDown` — skip resize and return original path when target is larger than source
- [ ] `onlyScaleUp` — skip resize and return original path when target is smaller than source
- [ ] `undefined` / absent — current behavior unchanged (no constraint)

## Out of Scope

- Allowing both constraints simultaneously (they are mutually exclusive by type)
- Any change to the `mode` (contain/cover/stretch) dimension math itself
- iOS native implementation (already stubs; `ScaleConstraint` plumbing still added to iOS `.mm` with same early-return guard)

---

## User Stories

### P1: onlyScaleDown ⭐ MVP

**User Story**: As a developer, I want to resize only when the image is larger than the target so that small images are never upscaled and returned blurry.

**Acceptance Criteria**:

1. WHEN `scaleConstraint: 'onlyScaleDown'` AND raw `targetWidth > srcWidth` OR `targetHeight > srcHeight` THEN system SHALL resolve with the original `filePath` unchanged
2. WHEN `scaleConstraint: 'onlyScaleDown'` AND `targetWidth <= srcWidth` AND `targetHeight <= srcHeight` THEN system SHALL perform the resize normally
3. WHEN `scaleConstraint: 'onlyScaleDown'` AND src and dst dims are equal THEN system SHALL perform the resize normally (equal is not a skip — caller may want quality change or rotation)

**Independent Test**: Call `resize()` with a 100×100 image, target 200×200, `onlyScaleDown` → returned path equals input path.

---

### P1: onlyScaleUp ⭐ MVP

**User Story**: As a developer, I want to resize only when the image is smaller than the target so that large originals are never downscaled unintentionally.

**Acceptance Criteria**:

1. WHEN `scaleConstraint: 'onlyScaleUp'` AND raw target dims are smaller than src dims in both axes THEN system SHALL resolve with the original `filePath` unchanged
2. WHEN `scaleConstraint: 'onlyScaleUp'` AND at least one raw target dim is larger than the corresponding src dim THEN system SHALL perform the resize normally
3. WHEN `scaleConstraint: 'onlyScaleUp'` AND src and dst dims are equal THEN system SHALL perform the resize normally (equal is not a skip)

**Independent Test**: Call `resize()` with a 400×300 image, target 200×150, `onlyScaleUp` → returned path equals input path.

---

### P1: No constraint (default) ⭐ MVP

**User Story**: As a developer, I want existing calls without `scaleConstraint` to behave exactly as before so that the feature is fully backwards-compatible.

**Acceptance Criteria**:

1. WHEN `scaleConstraint` is absent THEN system SHALL behave identically to current behavior
2. WHEN `scaleConstraint` is `undefined` THEN system SHALL behave identically to current behavior

**Independent Test**: All existing tests pass without modification.

---

## Edge Cases

- WHEN rotation is 90°/270° AND `scaleConstraint` is set THEN comparison uses raw target dims vs raw src bitmap dims (pre-rotation) — no axis swap; caller passes the logical target, bridge compares against raw decoded width/height
- WHEN mode is `stretch` AND `scaleConstraint: 'onlyScaleDown'` AND `targetWidth > srcWidth` OR `targetHeight > srcHeight` THEN system SHALL return original
- WHEN src and target dims are equal THEN resize proceeds normally (equal is not a skip)
- WHEN `scaleConstraint` value is an invalid string THEN JS layer SHALL reject with `TypeError: Invalid scaleConstraint: '<value>'`

---

## API Shape

```typescript
// New type (string literal union — one value at a time, no combined state)
export type ScaleConstraint = 'onlyScaleUp' | 'onlyScaleDown';

// Extended options bag
export interface ResizeOptions {
  rotation?: RotationAngle;
  mode?: ResizeMode;
  outputPath?: string;
  filterMode?: FilterMode;
  scaleConstraint?: ScaleConstraint;  // NEW
}
```

### Bridge contract

`scaleConstraint` is resolved **entirely in the JS/Kotlin layer before calling the native C++ resize**. The `NativeLibyuvResizer` spec (and iOS `.mm`) receive a new `scaleConstraint: string` parameter (empty string = unconstrained). The native layer passes it through to Kotlin; Kotlin checks dims after `computeDstDims` and short-circuits if constraint is violated.

> Passing it through the bridge (rather than handling it only in TS) keeps the constraint logic testable at the Kotlin unit-test level and prevents a future native-only caller from bypassing it.

---

## Success Criteria

- [ ] `onlyScaleDown` skips resize for smaller-or-equal source images — verified by unit + instrumented test
- [ ] `onlyScaleUp` skips resize for larger-or-equal source images — verified by unit + instrumented test
- [ ] All existing tests pass without modification
- [ ] `ScaleConstraint` type exported from `index.tsx`
- [ ] Invalid `scaleConstraint` value rejected with `TypeError` (JS layer)
