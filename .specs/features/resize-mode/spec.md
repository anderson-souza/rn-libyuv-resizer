# Resize Mode Specification

## Problem Statement

Current `resize()` always scales to exact `targetWidth × targetHeight` (stretch semantics). Callers must manually compute safe dimensions to preserve aspect ratio — a sharp pain point. Adding a `mode` option moves that math into the library and makes the common cases correct by default.

## Goals

- [ ] `contain` (default): output fits within `targetWidth × targetHeight`, preserving aspect ratio
- [ ] `cover`: output covers `targetWidth × targetHeight` (at least as wide OR as tall), preserving aspect ratio
- [ ] `stretch`: output is exactly `targetWidth × targetHeight` (current behavior, now explicit)
- [ ] No breaking change — callers that omit `mode` get `contain` (safe default; note: this changes current implicit-stretch behavior, see Out of Scope)

## Out of Scope

- Cropping / compositing (cover mode returns a scaled image, not a cropped one)
- Upscaling limits / quality policies
- New native implementation files — mode math added to existing Kotlin module, not a new module

---

## Rename: Remove `multiply` Scaffold Naming

### Problem Statement

`src/multiply.native.tsx` and `src/multiply.tsx` are scaffold artifacts from `react-native-builder-bob`. The name `multiply` is meaningless in this library and misleads contributors about the module's purpose.

### Goals

- [ ] Rename `src/multiply.native.tsx` → `src/resizer.native.tsx`
- [ ] Rename `src/multiply.tsx` → `src/resizer.tsx`
- [ ] Update all internal imports to use new filenames
- [ ] `src/index.tsx` re-exports from `'./resizer'` instead of `'./multiply'`

### Out of Scope

- Changes to `NativeLibyuvResizer.ts` (already well-named)
- Changes to native Android/iOS files

### Acceptance Criteria

1. WHEN build runs THEN no file named `multiply` SHALL exist under `src/`
2. WHEN `src/index.tsx` is read THEN all imports SHALL reference `'./resizer'`
3. WHEN `yarn typecheck` runs THEN it SHALL pass with zero errors after rename
4. WHEN `yarn test` runs THEN it SHALL pass with zero errors after rename

### Success Criteria

- [ ] `src/multiply.native.tsx` and `src/multiply.tsx` deleted
- [ ] `src/resizer.native.tsx` and `src/resizer.tsx` exist with identical logic
- [ ] Build, typecheck, and tests pass

---

## User Stories

### P1: Contain Mode ⭐ MVP

**User Story**: As a developer, I want to resize an image so it fits within a bounding box without distortion, so that thumbnails always look correct.

**Why P1**: Most common use case; the missing behavior causing callers to write their own math.

**Acceptance Criteria**:

1. WHEN `mode` is `'contain'` (or omitted) AND image is wider than tall THEN the output SHALL have `width = targetWidth` and `height = Math.round(targetWidth / aspectRatio)`, not exceeding `targetHeight`
2. WHEN `mode` is `'contain'` AND image is taller than wide THEN the output SHALL have `height = targetHeight` and `width = Math.round(targetHeight * aspectRatio)`, not exceeding `targetWidth`
3. WHEN `mode` is `'contain'` AND image already fits within bounds THEN the output SHALL not be upscaled — dimensions stay at the image's natural size
4. WHEN `mode` is omitted THEN system SHALL behave identically to `mode: 'contain'`

**Independent Test**: Pass a 1920×1080 image with `targetWidth=400, targetHeight=400` → output is 400×225.

---

### P1: Stretch Mode ⭐ MVP

**User Story**: As a developer, I want to resize to exact pixel dimensions regardless of aspect ratio, so I can fill a fixed grid slot.

**Why P1**: Preserves current implicit behavior under an explicit name; required for backwards-compat escape hatch.

**Acceptance Criteria**:

1. WHEN `mode` is `'stretch'` THEN the output SHALL be exactly `targetWidth × targetHeight`
2. WHEN `mode` is `'stretch'` THEN aspect ratio SHALL NOT be preserved

**Independent Test**: Pass a 1920×1080 image with `targetWidth=400, targetHeight=400, mode:'stretch'` → output is exactly 400×400.

---

### P2: Cover Mode

**User Story**: As a developer, I want to resize an image so it fully covers a target rectangle without distortion, so that hero images have no letterboxing.

**Why P2**: Common need but secondary to contain; no cropping so the returned image may be larger than the target box on one axis.

**Acceptance Criteria**:

1. WHEN `mode` is `'cover'` AND image is wider than tall THEN the output SHALL have `height = targetHeight` and `width = Math.round(targetHeight * aspectRatio)`, ensuring `width >= targetWidth`
2. WHEN `mode` is `'cover'` AND image is taller than wide THEN the output SHALL have `width = targetWidth` and `height = Math.round(targetWidth / aspectRatio)`, ensuring `height >= targetHeight`
3. WHEN `mode` is `'cover'` AND image aspect ratio matches the target THEN the output SHALL be exactly `targetWidth × targetHeight`

**Independent Test**: Pass a 1920×1080 image with `targetWidth=400, targetHeight=400, mode:'cover'` → output is 711×400 (height=400, width scaled up).

---

## Edge Cases

- WHEN `targetWidth` equals `targetHeight` (square target) AND image is non-square THEN contain SHALL produce a rectangle; cover SHALL produce a rectangle on the opposite axis
- WHEN computed dimension rounds to 0 THEN system SHALL clamp to 1 (minimum 1×1 output)
- WHEN image dimensions are equal to target dimensions THEN all modes SHALL pass through to native without scaling
- WHEN `mode` value is not one of the three valid strings THEN system SHALL throw a descriptive `TypeError` before calling native

---

## Success Criteria

- [ ] All three modes produce correct output dimensions verified by unit tests
- [ ] Existing callers that pass no `mode` get contain semantics (spec'd behavior, no silent stretch)
- [ ] TypeScript type for `mode` is a string literal union — no `any`, no magic strings at call sites
- [ ] Zero changes to native bridge (`NativeLibyuvResizer.ts` signature unchanged)
