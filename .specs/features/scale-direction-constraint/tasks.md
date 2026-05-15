# Scale Direction Constraint — Tasks

## Phase 1: Types & Bridge contract

- [ ] **T1** Add `ScaleConstraint` type and `VALID_SCALE_CONSTRAINTS` array to `src/resizer.native.tsx`
  - verify: `tsc` passes; type is `'onlyScaleUp' | 'onlyScaleDown'`

- [ ] **T2** Add `scaleConstraint?: ScaleConstraint` to `ResizeOptions` in `src/resizer.native.tsx`
  - verify: existing call sites in tests still compile without `scaleConstraint`

- [ ] **T3** Add validation + default (`''`) and pass `scaleConstraint` to bridge in `resize()` in `src/resizer.native.tsx`
  - verify: invalid value rejects with `TypeError: Invalid scaleConstraint: '...'`

- [ ] **T4** Add `scaleConstraint: string` as 9th param to `NativeLibyuvResizer.ts` spec
  - verify: `tsc` passes

- [ ] **T5** Export `ScaleConstraint` from `src/index.tsx`
  - verify: `import { ScaleConstraint } from 'react-native-libyuv-resizer'` resolves in TS

## Phase 2: Kotlin

- [ ] **T6** Add `scaleConstraint: String` field to `ResizeParams` data class
  - verify: all existing usages of `ResizeParams(...)` still compile (add default `""` or update callers)

- [ ] **T7** Add `scaleConstraint` validation to `ResizeValidator.validate()`
  - verify: `ResizeValidatorTest` passes for `""`, `"onlyScaleDown"`, `"onlyScaleUp"`, and invalid values

- [ ] **T8** Pass `scaleConstraint` from bridge method args through `ResizeParams` in `LibyuvResizerModule.kt`
  - verify: module compiles

- [ ] **T9** Add constraint check in `LibyuvResizerModule.kt` after `inJustDecodeBounds` decode, before full decode
  - Logic: `onlyScaleDown` → skip if `targetW > srcW || targetH > srcH`; `onlyScaleUp` → skip if `targetW < srcW || targetH < srcH`; equal → no skip
  - verify: unit test for each branch (see test table in design.md)

## Phase 3: iOS stub update

- [ ] **T10** Add `scaleConstraint:(NSString *)scaleConstraint` param to `LibyuvResizer.mm` method signature
  - verify: iOS build compiles (no behavior change, still rejects E_NOT_IMPLEMENTED)

## Phase 4: Tests

- [ ] **T11** Add `ResizeValidatorTest` cases for `scaleConstraint` field
  - valid: `""`, `"onlyScaleDown"`, `"onlyScaleUp"`
  - invalid: `"both"`, `"ONLYSCALEUP"`, any other string

- [ ] **T12** Add instrumented tests for `LibyuvResizerModule` constraint short-circuit (8 cases from design.md test table)

- [ ] **T13** Add JS unit test: invalid `scaleConstraint` value → `TypeError`

## Phase 5: Verify

- [ ] **T14** Run `yarn typecheck` — zero errors
- [ ] **T15** Run `yarn test` — all pass
- [ ] **T16** Run Android instrumented tests — all pass
- [ ] **T17** Confirm all existing tests pass unmodified
