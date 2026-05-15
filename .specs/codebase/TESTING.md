# Testing Infrastructure

## Test Frameworks

- **Unit (JS):** Jest 30 with `@react-native/jest-preset`
- **Unit (Android JVM):** JUnit via Gradle `test` task — no emulator needed
- **Integration (Android):** Instrumented tests (`androidTest`) — requires emulator/device
- **E2E:** None
- **Coverage:** `--coverage` flag; **85% threshold enforced** in Jest config

## Test Organization

**JS tests:**  
Location: `src/__tests__/`  
Naming: `*.test.tsx`, `*.test.ts`  
Files: `index.test.tsx`, `NativeLibyuvResizer.test.ts`

**Android JVM unit tests:**  
Location: `android/src/test/java/com/libyuvresizer/`  
Files: `DimensionCalculatorTest.kt`, `ResizeValidatorTest.kt`

**Android instrumented tests:**  
Location: `android/src/androidTest/java/com/libyuvresizer/`  
Files: `LibyuvResizerModuleIntegrationTest`, `ErrorTest`, `FilterModeTest`, `OutputPathTest`, `RotationTest`  
Helpers: `FakePromise.kt`, `FakeReactContext.kt`, `TestFixtures.kt`

## Testing Patterns

### JS Unit Tests

**Approach:** Jest with explicit `@jest/globals` imports; mocks `NativeModules` for arch-detection tests.

```typescript
import { describe, it, expect, jest } from '@jest/globals';
// NativeLibyuvResizer.test.ts — tests arch guard + legacy bridge fallback
```

**Jest config (from `package.json`):**
```json
{
  "preset": "@react-native/jest-preset",
  "coverageThreshold": { "global": { "lines": 85 } },
  "modulePathIgnorePatterns": [
    "<rootDir>/example/node_modules",
    "<rootDir>/lib/"
  ]
}
```

### Android JVM Unit Tests

**Approach:** Pure Kotlin tests for logic extracted into `DimensionCalculator` and `ResizeValidator` — no Android runtime, no emulator.

```kotlin
// DimensionCalculatorTest.kt — tests contain/cover/stretch dimension math
// ResizeValidatorTest.kt — tests validation error codes and messages
```

### Android Instrumented Tests

**Approach:** `FakePromise` + `FakeReactContext` stand-ins allow testing `LibyuvResizerModule` end-to-end on-device without a full RN host.

```kotlin
// LibyuvResizerModuleIntegrationTest — full resize call on real bitmap
// LibyuvResizerModuleRotationTest — verifies rotation swaps dimensions
// LibyuvResizerModuleFilterModeTest — verifies all 4 filter modes accepted
// LibyuvResizerModuleOutputPathTest — verifies custom outputPath logic
// LibyuvResizerModuleErrorTest — verifies reject codes for bad inputs
```

### Integration Tests (JS layer)

None. Native-side integration tested via Android instrumented tests + CI `build:android`.

### E2E Tests

None. Manual testing via `example/` app.

## Test Execution

```bash
# JS
yarn test                                  # all JS tests
yarn test --maxWorkers=2 --coverage        # CI mode with 85% threshold
yarn test --testPathPattern="NativeLibyuv" # single file

# Android JVM (from example/android/)
./gradlew :react-native-libyuv-resizer:test

# Android instrumented (requires emulator/device)
./gradlew :react-native-libyuv-resizer:connectedAndroidTest
```

## Coverage Targets

- **JS:** 85% lines — enforced in Jest config, gates CI `build-library` job
- **Android:** No automated threshold; JVM unit tests cover `DimensionCalculator` + `ResizeValidator`; instrumented tests cover `LibyuvResizerModule`
