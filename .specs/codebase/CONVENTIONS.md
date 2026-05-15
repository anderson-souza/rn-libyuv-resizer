# Code Conventions

## Naming Conventions

**Files:**
- TypeScript: `PascalCase` for specs (`NativeLibyuvResizer.ts`), `camelCase.native.tsx` for platform splits (`resizer.native.tsx`)
- Kotlin: `PascalCase` matching class name (`LibyuvResizerModule.kt`, `DimensionCalculator.kt`, `ResizeValidator.kt`)
- ObjC++: `PascalCase` matching class (`LibyuvResizer.mm`, `LibyuvResizer.h`)

**Functions/Methods:**
- TypeScript: `camelCase` (`resize`, `toCanonicalAngle`)
- Kotlin: `camelCase` (`override fun resize(...)`, `computeDstDims`, `validate`)
- ObjC++: selector style (`- (void)resize:(NSString *)filePath ...`)

**Constants:**
- Kotlin: `const val NAME` in `companion object` — follows RN convention; `SCREAMING_SNAKE` for private maps (`FILTER_MODE_MAP`)

**Module name string:** `"LibyuvResizer"` — must match across `TurboModuleRegistry.getEnforcing`, `LibyuvResizerModule.NAME`, and `+ (NSString *)moduleName`.

## Code Organization

**Import ordering (TypeScript):**  
React Native imports first, then local imports.

**File structure (Kotlin):**
```kotlin
package com.libyuvresizer

// imports

class LibyuvResizerModule(reactContext: ReactApplicationContext) :
  NativeLibyuvResizerSpec(reactContext) {

  companion object {
    const val NAME = NativeLibyuvResizerSpec.NAME
    init { System.loadLibrary("libyuvresizer") }
  }

  private external fun nativeResize(...)
  override fun resize(..., promise: Promise) { ... }
}
```

**Kotlin utilities extracted for testability:**  
Pure logic (no Android deps) lives in `DimensionCalculator` and `ResizeValidator` — `LibyuvResizerModule` calls into them.

## Type Safety

- TypeScript: strict mode via `tsconfig.json`; no `any` in spec files; string literal unions for `ResizeMode`, `FilterMode`, `RotationAngle`
- Kotlin: null-safe; sealed `ValidationResult` eliminates unchecked error handling; `requireNotNull` over `!!`
- ObjC++: codegen-generated spec header enforced; no `id` returns on typed methods

## Error Handling

- Web/non-native fallback (`resizer.tsx`): throws `Error("'react-native-libyuv-resizer' is only supported on native platforms.")`
- JS validation (`resizer.native.tsx`): `Promise.reject(new TypeError(...))` for invalid `mode` / `filterMode`
- Android: `ResizeValidator` returns `ValidationResult.Invalid(code, message)`; module calls `promise.reject(code, message)` and returns early
- `TurboModuleRegistry.getEnforcing` — throws at startup if native side missing

## Commit Style

Conventional commits enforced by commitlint + lefthook:
```
feat: add filterMode param to resize API
fix: initialize libyuv submodule on Android CI checkout
chore: update CLAUDE.md dev guidelines
```
Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

## Prettier Config (from `package.json`)

```json
{
  "quoteProps": "consistent",
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "useTabs": false
}
```

## Sentinel Values

- `outputPath = ''` (empty string) signals "no custom path" — TurboModule bridge requires fixed-arity positional args; nullable strings are not supported.
- `rotation = 0` default — JS normalises negative angles to `0|90|180|270` before sending to native.
- `quality = 100` → PNG output; else JPEG — single `quality` param drives format selection without a separate API param.
