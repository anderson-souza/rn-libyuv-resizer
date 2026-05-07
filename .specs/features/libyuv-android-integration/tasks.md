# libyuv Android Integration — Tasks

**Design**: `.specs/features/libyuv-android-integration/design.md`
**Status**: Done

---

## Execution Plan

```
Phase 1 (Sequential — foundation):
  T1 → T2 → T3

Phase 2 (Parallel — core impl, all unblock after T3):
  T3 complete, then:
    ├── T4 [P]  C++ JNI implementation
    └── T5 [P]  Kotlin resize() implementation

Phase 3 (Sequential — wire + build):
  T4 + T5 complete, then:
    T6 → T7 → T8
```

---

## Task Breakdown

### T1: Update TypeScript Turbo Module spec

**What**: Replace `multiply` with `resize` in `NativeLibyuvResizer.ts`
**Where**: `src/NativeLibyuvResizer.ts`
**Depends on**: None

**Done when**:
- [ ] `multiply` removed
- [ ] `resize(filePath: string, targetWidth: number, targetHeight: number, quality: number): Promise<string>` exported
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T2: Update JS/web fallback

**What**: Replace `multiply` stub with `resize` stub in `multiply.tsx`
**Where**: `src/multiply.tsx`
**Depends on**: T1

**Done when**:
- [ ] `multiply` export removed
- [ ] `resize` exported with same signature as T1 spec (returns `Promise.reject` with `"Not supported on web"`)
- [ ] `yarn typecheck` passes

**Verify**:
```bash
yarn typecheck
```

---

### T3: Update `index.tsx` public exports

**What**: Re-export `resize` instead of `multiply`
**Where**: `src/index.tsx`
**Depends on**: T1, T2

**Done when**:
- [ ] `multiply` export removed
- [ ] `resize` re-exported from `multiply.native` / `multiply`
- [ ] `yarn typecheck` passes
- [ ] `yarn prepare` (builds `lib/`) exits 0

**Verify**:
```bash
yarn typecheck && yarn prepare
```

---

### T4: Create C++ JNI implementation [P]

**What**: Create `LibyuvResizerModule.cpp` — JNI bridge that locks Bitmap pixels and calls `libyuv::ARGBScale`
**Where**: `android/src/main/cpp/LibyuvResizerModule.cpp` (new file)
**Depends on**: T3

**Implementation**:
```cpp
#include <jni.h>
#include <android/bitmap.h>
#include "libyuv/scale_argb.h"

extern "C" JNIEXPORT void JNICALL
Java_com_libyuvresizer_LibyuvResizerModule_nativeResize(
    JNIEnv* env,
    jobject /* thiz */,
    jobject srcBitmap,
    jobject dstBitmap
) {
    AndroidBitmapInfo srcInfo{}, dstInfo{};
    AndroidBitmap_getInfo(env, srcBitmap, &srcInfo);
    AndroidBitmap_getInfo(env, dstBitmap, &dstInfo);

    void* srcPixels = nullptr;
    void* dstPixels = nullptr;

    if (AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels) < 0 ||
        AndroidBitmap_lockPixels(env, dstBitmap, &dstPixels) < 0) {
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "AndroidBitmap_lockPixels failed");
        return;
    }

    libyuv::ARGBScale(
        static_cast<const uint8_t*>(srcPixels), srcInfo.stride,
        srcInfo.width,  srcInfo.height,
        static_cast<uint8_t*>(dstPixels),       dstInfo.stride,
        dstInfo.width,  dstInfo.height,
        libyuv::kFilterBox
    );

    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);
}
```

**Done when**:
- [ ] File created at correct path
- [ ] JNI symbol matches Kotlin package `com.libyuvresizer.LibyuvResizerModule.nativeResize`
- [ ] `lockPixels` failure throws `RuntimeException` back to JVM
- [ ] No raw `new`/`delete`, no heap allocation

---

### T5: Implement Kotlin `resize()` method [P]

**What**: Replace `multiply` with full `resize` implementation in `LibyuvResizerModule.kt`
**Where**: `android/src/main/java/com/libyuvresizer/LibyuvResizerModule.kt`
**Depends on**: T3

**Implementation outline**:
```kotlin
class LibyuvResizerModule(reactContext: ReactApplicationContext) :
    NativeLibyuvResizerSpec(reactContext) {

    override fun resize(
        filePath: String,
        targetWidth: Double,
        targetHeight: Double,
        quality: Double,
        promise: Promise
    ) {
        try {
            // 1. Validate
            if (!File(filePath).exists()) {
                promise.reject("E_FILE_NOT_FOUND", "File not found: $filePath"); return
            }
            val dstW = targetWidth.toInt()
            val dstH = targetHeight.toInt()
            val q = quality.toInt()
            if (dstW <= 0 || dstH <= 0) {
                promise.reject("E_INVALID_DIMS", "Invalid dimensions"); return
            }
            if (q < 1 || q > 100) {
                promise.reject("E_INVALID_QUALITY", "Quality must be between 1 and 100"); return
            }

            // 2. Decode
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val srcBitmap = BitmapFactory.decodeFile(filePath, opts)
                ?: run { promise.reject("E_DECODE_FAILED", "Failed to decode image"); return }

            // 3. Allocate dst
            val dstBitmap = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)

            // 4. Scale (C++ writes into dstBitmap directly)
            nativeResize(srcBitmap, dstBitmap)
            srcBitmap.recycle()

            // 5. Encode
            val ext = if (q == 100) "png" else "jpg"
            val outFile = File(reactApplicationContext.cacheDir, "${UUID.randomUUID()}.$ext")
            FileOutputStream(outFile).use { fos ->
                val fmt = if (q == 100) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                dstBitmap.compress(fmt, q, fos)
            }
            dstBitmap.recycle()

            promise.resolve(outFile.absolutePath)
        } catch (e: Exception) {
            promise.reject("E_UNKNOWN", e.message)
        }
    }

    private external fun nativeResize(srcBitmap: Bitmap, dstBitmap: Bitmap)

    companion object {
        const val NAME = NativeLibyuvResizerSpec.NAME
        init { System.loadLibrary("libyuvresizer") }
    }
}
```

**Done when**:
- [ ] `multiply` removed
- [ ] All 5 error cases reject with correct codes (see design error table)
- [ ] `srcBitmap.recycle()` called before encode
- [ ] `dstBitmap.recycle()` called after encode
- [ ] `nativeResize` declared `external`
- [ ] `loadLibrary("libyuvresizer")` in companion `init`

---

### T6: Update CMakeLists.txt

**What**: Add `jnigraphics` to `target_link_libraries`
**Where**: `android/CMakeLists.txt`
**Depends on**: T4, T5

**Change**:
```cmake
target_link_libraries(
    ${PACKAGE_NAME}
    ${LOG_LIB}
    android
    jnigraphics
    yuv
)
```

**Done when**:
- [ ] `jnigraphics` present in `target_link_libraries`
- [ ] No other changes to CMakeLists.txt

---

### T7: Android build green

**What**: Verify `assembleDebug` compiles all 4 ABIs without errors
**Where**: `example/` (trigger build from example app)
**Depends on**: T6

**Done when**:
- [ ] `cd example && ./gradlew :rn-libyuv-resizer:assembleDebug` exits 0
- [ ] All 4 ABI `.so` files present under `android/build/intermediates/cmake/`
- [ ] No CMake errors, no linker errors, no JNI symbol warnings

**Verify**:
```bash
cd example
./gradlew :rn-libyuv-resizer:assembleDebug 2>&1 | tail -20
find ../android/build -name "*.so" | sort
```

---

### T8: Manual end-to-end smoke test

**What**: Call `resize` from the example app on Android and verify output
**Where**: `example/src/App.tsx` (add smoke test call)
**Depends on**: T7

**Done when**:
- [ ] Example app calls `resize(testImagePath, 320, 240, 80)`
- [ ] Promise resolves (no rejection)
- [ ] Output file exists at returned path
- [ ] Output file dimensions are 320×240 (verify with any image tool or `ExifInterface`)
- [ ] Output file size < input file size (downscale confirmation)

**Verify**:
Run example app on Android emulator (arm64-v8a or x86_64), observe console logs / UI showing output path.

---

## Parallel Execution Map

```
T1 ──→ T2 ──→ T3 ──┬──→ T4 [P] ──┬──→ T6 ──→ T7 ──→ T8
                    └──→ T5 [P] ──┘
```

T4 and T5 are fully independent — can be implemented simultaneously by separate agents.

---

## Granularity Check

| Task | Scope | Status |
|------|-------|--------|
| T1: Update TS spec | 1 file, replace 1 method | ✅ |
| T2: Update web fallback | 1 file, replace 1 export | ✅ |
| T3: Update index exports | 1 file, swap re-export | ✅ |
| T4: C++ JNI impl | 1 new file, 1 function | ✅ |
| T5: Kotlin resize() | 1 file, replace 1 method | ✅ |
| T6: CMakeLists patch | 1 line change | ✅ |
| T7: Build verification | run + check | ✅ |
| T8: E2E smoke test | 1 call, observe output | ✅ |
