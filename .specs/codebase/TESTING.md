# Testing Infrastructure

## Test Frameworks

- **Unit:** Jest 30 with `@react-native/jest-preset`
- **E2E:** None
- **Coverage:** `--coverage` flag available; no enforced threshold

## Test Organization

**Location:** `src/__tests__/`  
**Naming:** `*.test.tsx`  
**Structure:** Co-located with source in `src/__tests__/`

## Testing Patterns

### Unit Tests

**Current state:** Scaffold only — `src/__tests__/index.test.tsx` contains `it.todo('write a test')`.  
**Approach when implemented:** Jest + `@jest/globals` explicit imports (not globals).

```typescript
import { it } from '@jest/globals';

it.todo('write a test');
```

**Jest config (from `package.json`):**
```json
{
  "preset": "@react-native/jest-preset",
  "modulePathIgnorePatterns": [
    "<rootDir>/example/node_modules",
    "<rootDir>/lib/"
  ]
}
```

### Integration Tests

None present. Native-side integration tested indirectly via CI `build:android` and `build:ios` tasks.

### E2E Tests

None. Manual testing via `example/` app.

## Test Execution

```bash
yarn test                               # all tests
yarn test --maxWorkers=2 --coverage     # CI mode with coverage
yarn test --testPathPattern="src/__tests__/index"  # single file
```

## Coverage Targets

- **Current:** No measured baseline (only todo stubs exist)
- **Goals:** Not yet documented
- **Enforcement:** None automated — coverage run in CI but no threshold gate
