# Code Conventions

## Naming Conventions

**Files:**
- TypeScript: `PascalCase` for specs/components (`NativeLibyuvResizer.ts`), `camelCase.native.tsx` for platform splits
- Kotlin: `PascalCase` matching class name (`LibyuvResizerModule.kt`)
- ObjC++: `PascalCase` matching class (`LibyuvResizer.mm`, `LibyuvResizer.h`)

**Functions/Methods:**
- TypeScript: `camelCase` (`multiply`, `resizeImage`)
- Kotlin: `camelCase` (`override fun multiply(a: Double, b: Double)`)
- ObjC++: selector style (`- (NSNumber *)multiply:(double)a b:(double)b`)

**Constants:**
- Kotlin: `const val NAME` in `companion object` — SCREAMING_SNAKE not used; `NAME` follows RN convention

**Module name string:** `"LibyuvResizer"` — used as the JS-side TurboModule registry key and must match across `TurboModuleRegistry.getEnforcing`, `LibyuvResizerModule.NAME`, and `+ (NSString *)moduleName`.

## Code Organization

**Import ordering (TypeScript):**  
React Native imports first, then local imports. No explicit grouping separator observed yet.

**File structure (Kotlin):**
```kotlin
package com.libyuvresizer

// imports

class LibyuvResizerModule(reactContext: ReactApplicationContext) :
  NativeLibyuvResizerSpec(reactContext) {

  override fun methodName(...): ReturnType { ... }

  companion object {
    const val NAME = NativeLibyuvResizerSpec.NAME
  }
}
```

## Type Safety

- TypeScript: strict mode via `tsconfig.json`; no `any` in spec files
- Kotlin: null-safe; native params from codegen are non-nullable primitives
- ObjC++: codegen-generated spec header enforced; no `id` returns on typed methods

## Error Handling

- Web/non-native fallback (`multiply.tsx`): throws `Error` with explicit message `"'react-native-libyuv-resizer' is only supported on native platforms."`
- Turbo Module: `TurboModuleRegistry.getEnforcing` — throws at startup if native side missing (fail-fast, not silent)

## Commit Style

Conventional commits enforced by commitlint + lefthook:
```
feat: add image resize API
fix: correct rotation matrix on arm64
chore: update libyuv submodule to daeff19
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
