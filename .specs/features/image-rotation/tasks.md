# Image Rotation Tasks

**Design**: `.specs/features/image-rotation/design.md`
**Status**: Draft
**Scope**: Android only — iOS deferred

---

## Execution Plan

```
Phase 1 (Sequential — spec contract first):
  T1 → T2

Phase 2 (Parallel — independent layers):
  T2 complete, then:
    ├── T3 [P]  (C++ nativeRotate JNI)
    └── T4 [P]  (Kotlin orchestration)

Phase 3 (Sequential — wire everything):
  T3 + T4 complete, then:
    T5 → T6
```

---

## Task Breakdown

### T1: Extend Turbo Module spec with `rotation` param

**What**: Add `rotation: number` as 5th param to `Spec.resize` in `NativeLibyuvResizer.ts`
**Where**: `src/NativeLibyuvResizer.ts`
**Depends on**: None

**Done when**:
- [ ] `resize` signature has 5th param `rotation: number`
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T2: Extend JS public API with `ResizeOptions` and angle normalization

**What**: Update `multiply.native.tsx` + `multiply.tsx` + `index.tsx` — add `RotationAngle` type, `ResizeOptions` interface, normalize negative angles, pass canonical rotation to native
**Where**: `src/multiply.native.tsx`, `src/multiply.tsx`, `src/index.tsx`
**Depends on**: T1

**Change in `multiply.native.tsx`**:
```typescript
type RotationAngle = 0 | 90 | 180 | 270 | -90 | -180 | -270;

export interface ResizeOptions {
  rotation?: RotationAngle;
}

function toCanonicalAngle(angle: RotationAngle): 0 | 90 | 180 | 270 {
  return (((angle % 360) + 360) % 360) as 0 | 90 | 180 | 270;
}

export function resize(
  filePath: string,
  targetWidth: number,
  targetHeight: number,
  quality: number,
  options?: ResizeOptions
): Promise<string> {
  const rotation = options?.rotation != null
    ? toCanonicalAngle(options.rotation)
    : 0;
  return LibyuvResizer.resize(filePath, targetWidth, targetHeight, quality, rotation);
}
```

**Done when**:
- [ ] `RotationAngle` and `ResizeOptions` exported from `src/index.tsx`
- [ ] `toCanonicalAngle(-90)` returns `270`, `toCanonicalAngle(-180)` returns `180`, `toCanonicalAngle(-270)` returns `90`
- [ ] Omitting `options` sends `rotation=0` to native
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T3: Add `nativeResizeAndRotate` JNI function in C++ [P]

**What**: Add `Java_com_libyuvresizer_LibyuvResizerModule_nativeResizeAndRotate` to `LibyuvResizerModule.cpp` — single JNI call that scales to a `malloc`'d intermediate buffer then rotates into `dstBitmap`. No intermediate `Bitmap` object.
**Where**: `android/src/main/cpp/LibyuvResizerModule.cpp`
**Depends on**: T1

**Implementation**:
```cpp
#include "libyuv/rotate_argb.h"
#include <cstdlib>

extern "C" JNIEXPORT void JNICALL
Java_com_libyuvresizer_LibyuvResizerModule_nativeResizeAndRotate(
    JNIEnv* env,
    jobject /* thiz */,
    jobject srcBitmap,
    jobject dstBitmap,
    jint rotation
) {
    AndroidBitmapInfo srcInfo{}, dstInfo{};
    AndroidBitmap_getInfo(env, srcBitmap, &srcInfo);
    AndroidBitmap_getInfo(env, dstBitmap, &dstInfo);

    // pre-rotation dims: 90/270 swap width/height
    const bool swap = (rotation == 90 || rotation == 270);
    const uint32_t preW = swap ? dstInfo.height : dstInfo.width;
    const uint32_t preH = swap ? dstInfo.width  : dstInfo.height;
    const uint32_t preStride = preW * 4; // ARGB_8888

    uint8_t* preBuf = static_cast<uint8_t*>(std::malloc(preStride * preH));
    if (!preBuf) {
        jclass ex = env->FindClass("java/lang/OutOfMemoryError");
        env->ThrowNew(ex, "nativeResizeAndRotate: malloc failed");
        return;
    }

    void* srcPixels = nullptr;
    void* dstPixels = nullptr;

    if (AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels) < 0 ||
        AndroidBitmap_lockPixels(env, dstBitmap, &dstPixels) < 0) {
        std::free(preBuf);
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "AndroidBitmap_lockPixels failed");
        return;
    }

    // Step 1: scale src → intermediate malloc buffer
    libyuv::ARGBScale(
        static_cast<const uint8_t*>(srcPixels), srcInfo.stride,
        srcInfo.width, srcInfo.height,
        preBuf, preStride,
        preW, preH,
        libyuv::kFilterBox
    );

    // Step 2: rotate intermediate → dst Bitmap
    libyuv::RotationMode mode = libyuv::kRotate0;
    if (rotation == 90)  mode = libyuv::kRotate90;
    if (rotation == 180) mode = libyuv::kRotate180;
    if (rotation == 270) mode = libyuv::kRotate270;

    libyuv::ARGBRotate(
        preBuf, preStride,
        static_cast<uint8_t*>(dstPixels), dstInfo.stride,
        preW, preH,
        mode
    );

    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);
    std::free(preBuf);
}
```

**Peak memory**: `src pixels` + `malloc(preW×preH×4)` + `dst pixels` — intermediate is freed before JNI returns; no Android `Bitmap` wrapper overhead.

**Done when**:
- [ ] Compiles without error (`yarn turbo run build:android`)
- [ ] `malloc` failure throws `OutOfMemoryError`
- [ ] `lockPixels` failure frees `preBuf` before throwing `RuntimeException`
- [ ] `preBuf` freed on all exit paths

**Verify**:
```bash
yarn turbo run build:android
```

---

### T4: Update `LibyuvResizerModule.kt` — orchestrate scale + rotate [P]

**What**: Extend `resize` to accept `rotation: Double`; for rotation≠0 call single `nativeResizeAndRotate(srcBitmap, dstBitmap, rot)` — only 2 Bitmap objects total (src + dst)
**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
**Depends on**: T1

**Key changes**:
1. Override signature gains `rotation: Double` param
2. Validate `rotation.toInt() in setOf(0, 90, 180, 270)` → reject `E_INVALID_ROTATION`
3. Fast path `rotation == 0`: `dstBitmap(dstW, dstH)` → `nativeResize(src, dst)` (unchanged)
4. Rotate path:
   ```kotlin
   val rot = rotation.toInt()
   val dstBitmap = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
   nativeResizeAndRotate(srcBitmap, dstBitmap, rot)
   // srcBitmap recycled after; no intermediate Bitmap ever created
   ```
5. Add `private external fun nativeResizeAndRotate(srcBitmap: Bitmap, dstBitmap: Bitmap, rotation: Int)`

**Bitmap lifetime**:
```
rotation=0:  srcBitmap → nativeResize → dstBitmap → recycle(src) → encode → recycle(dst)
rotation≠0:  srcBitmap → nativeResizeAndRotate → dstBitmap → recycle(src) → encode → recycle(dst)
             (intermediate lives only inside C++, freed before JNI returns)
```

**Done when**:
- [ ] `rotation=0` identical output to current `resize` (no regression)
- [ ] `rotation=90` on landscape src targeting portrait dims → correct portrait output
- [ ] Invalid rotation (e.g. 45) → `E_INVALID_ROTATION` reject
- [ ] No intermediate `Bitmap` created in Kotlin for rotation path
- [ ] `yarn turbo run build:android` succeeds

**Verify**:
```bash
yarn turbo run build:android
```

---

### T5: Wire in example app — add rotation test UI

**What**: Add rotation test button/call in `example/src/App.tsx` to exercise the new param end-to-end
**Where**: `example/src/App.tsx`
**Depends on**: T3, T4

**Done when**:
- [ ] Example app calls `resize(uri, 540, 960, 80, { rotation: 90 })` and displays result
- [ ] Example app calls `resize(uri, 540, 960, 80)` (no rotation) and displays result — both work without crash

**Verify**: Run example app on Android emulator/device; both calls produce images with correct orientation.

---

### T6: Update STATE.md

**What**: Record rotation feature as implemented on Android; note iOS deferred
**Where**: `.specs/project/STATE.md`
**Depends on**: T5

**Done when**:
- [ ] STATE.md Active Work reflects rotation tasks complete
- [ ] iOS rotation noted as deferred (out of scope for this iteration)

---

## Parallel Execution Map

```
Phase 1 (Sequential):
  T1 → T2

Phase 2 (Parallel — start after T1 done):
  T1 complete, then:
    ├── T3 [P]  C++ nativeRotate
    └── T4 [P]  Kotlin orchestration
  (T2 can also run in parallel with T3/T4 — no dependency between them after T1)

Phase 3 (Sequential):
  T3 + T4 complete, then:
    T5 → T6
```

## Granularity Check

| Task | Scope | Status |
|------|-------|--------|
| T1: Extend TS spec | 1 file, 1 param | ✅ |
| T2: JS API + normalization | 3 files, cohesive | ✅ |
| T3: C++ nativeResizeAndRotate | 1 function in 1 file | ✅ |
| T4: Kotlin orchestration | 1 method update | ✅ |
| T5: Example app wiring | 1 file, smoke test | ✅ |
| T6: STATE.md update | 1 doc | ✅ |
