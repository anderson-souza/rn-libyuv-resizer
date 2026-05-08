# Custom Output Path Specification

## Problem Statement

`resize()` always saves the processed image to the app's default temp/cache directory. Apps that need to write to a specific directory (e.g., app-controlled gallery, Documents, external storage) must perform an extra file move after every call, adding latency and complexity.

## Goals

- [ ] Allow caller to specify a target directory for the resized image output
- [ ] Keep backward compat: omitting the param preserves current behavior

## Out of Scope

- Directory creation (caller is responsible for ensuring the directory exists)
- Custom output filename (output filename always mirrors the input filename)
- Permission management (caller must handle platform storage permissions)

---

## User Stories

### P1: Optional outputPath parameter ⭐ MVP

**User Story**: As a React Native developer, I want to pass an optional `outputPath` directory to `resize()` so that the processed image is saved directly where my app needs it, without an extra file-copy step.

**Why P1**: Core ask; no other story makes sense without this.

**Acceptance Criteria**:

1. WHEN `outputPath` is omitted THEN system SHALL save to the default temp/cache directory (existing behavior unchanged)
2. WHEN `outputPath` is a valid, existing directory path THEN system SHALL save the resized image there using the original input filename
3. WHEN `outputPath` points to a non-existent directory THEN system SHALL reject with a clear error: `"Output directory does not exist: <path>"`
4. WHEN `outputPath` is an empty string THEN system SHALL treat it as omitted (use default)
5. WHEN the resized image is saved to `outputPath` THEN system SHALL return the full absolute path of the saved file

**Independent Test**: Call `resize(inputPath, w, h, q, r, mode, '/tmp/custom-dir')` → file appears at `/tmp/custom-dir/<original-filename>` and returned string matches that path.

---

## Edge Cases

- WHEN input filename has no extension THEN system SHALL save as-is (no extension added)
- WHEN two calls share the same `outputPath` and same input filename THEN system SHALL overwrite silently (no error)
- WHEN `outputPath` is a file path (not a directory) THEN system SHALL reject with `"outputPath must be a directory, not a file: <path>"`
- WHEN `outputPath` contains a trailing slash THEN system SHALL handle correctly (no double-slash in output path)

---

## Success Criteria

- [ ] Existing callers with no `outputPath` see zero behavior change
- [ ] Callers passing `outputPath` get the file at `<outputPath>/<input-filename>` in one call
- [ ] All error cases return a rejected Promise with a descriptive message (no silent failures)
- [ ] Android + iOS both implement the param
