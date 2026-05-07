# Image Rotation Specification

## Problem Statement

`resize` only scales images; no rotation. Apps processing camera output (portrait/landscape) must rotate before/after calling into native — adding an extra encode/decode round-trip. Rotation should happen in the same native pass as resize, with zero extra file I/O.

## Goals

- [ ] Extend `resize` API to accept an optional rotation angle
- [ ] Perform rotate + scale in a single native call on Android and iOS
- [ ] Support angles: 90, 180, 270 and their negative equivalents (-90, -180, -270)
- [ ] Normalize all input angles to canonical 90/180/270 before passing to native

## Out of Scope

- Arbitrary/free angles (not 0/90/180/270 multiples)
- Flip (horizontal/vertical mirror)
- Standalone `rotate()` function — combined only

---

## User Stories

### P1: Rotate + Resize in Single Call ⭐ MVP

**User Story**: As an RN developer, I want to pass an optional `rotation` to `resize` so that I can correct camera orientation and scale in one native operation without extra I/O.

**Why P1**: Core feature request; no partial value without it.

**Acceptance Criteria**:

1. WHEN `resize(filePath, w, h, quality, { rotation: 90 })` is called THEN system SHALL rotate the image 90° clockwise AND scale to `w×h` in a single native pass
2. WHEN `rotation` is omitted or `0` THEN system SHALL behave identically to current `resize` (no regression)
3. WHEN `rotation: -90` is passed THEN system SHALL normalize to 270° and rotate accordingly
4. WHEN `rotation: -180` is passed THEN system SHALL normalize to 180°
5. WHEN `rotation: -270` is passed THEN system SHALL normalize to 90°
6. WHEN `rotation` is a value not in `{-270,-180,-90,0,90,180,270}` THEN system SHALL reject with a descriptive error
7. WHEN rotation changes image orientation (90 or 270) THEN system SHALL swap width/height for the scale step so `targetWidth`/`targetHeight` refer to the **final** output dimensions

**Independent Test**: Call `resize` with `rotation: 90` on a 1920×1080 landscape image targeting 540×960 — output SHALL be 540×960 portrait.

---

### P2: TypeScript Types Enforce Valid Angles

**User Story**: As an RN developer, I want TypeScript to reject invalid rotation values at compile time so that I catch misuse before runtime.

**Why P2**: DX improvement; runtime validation covers correctness, TS covers ergonomics.

**Acceptance Criteria**:

1. WHEN `rotation` option type is `0 | 90 | 180 | 270 | -90 | -180 | -270` THEN TS SHALL error on any other numeric value
2. WHEN options object is omitted entirely THEN TS SHALL accept the call (options are optional)

**Independent Test**: `resize(path, 100, 100, 80, { rotation: 45 })` fails `tsc`; `resize(path, 100, 100, 80)` passes.

---

## Edge Cases

- WHEN `rotation: 0` explicitly passed THEN system SHALL skip rotation logic entirely (no-op path)
- WHEN source image is square AND rotation is 90/270 THEN system SHALL still produce correct output (w/h swap is a no-op on dimensions but rotation still applied)
- WHEN `filePath` is invalid AND rotation is set THEN system SHALL reject with file-not-found error (same as current behavior)
- WHEN `quality` is 100 AND rotation set THEN system SHALL output PNG (unchanged from current lossless logic)

---

## Success Criteria

- [ ] `resize(path, w, h, q, { rotation: 90 })` produces correctly rotated + scaled image on Android and iOS
- [ ] `resize(path, w, h, q)` (no rotation) produces identical output to current implementation — zero regression
- [ ] Invalid angle throws a JS-catchable error with message including the invalid value
- [ ] TypeScript rejects invalid angles at compile time
- [ ] Single file read + single file write — no intermediate temp files
