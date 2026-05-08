#include <jni.h>
#include <android/bitmap.h>
#include <cstdlib>
#include <climits>
#include "libyuv/scale_argb.h"
#include "libyuv/rotate_argb.h"

namespace {

// Aligned to 64 bytes (cache line) to help libyuv SIMD paths.
// Uses posix_memalign — available on Android since API 8.
struct AlignedBuf {
    uint8_t* ptr;
    explicit AlignedBuf(size_t n) : ptr(nullptr) {
        void* p = nullptr;
        if (posix_memalign(&p, 64, n) == 0)
            ptr = static_cast<uint8_t*>(p);
    }
    ~AlignedBuf() { std::free(ptr); }
    AlignedBuf(const AlignedBuf&) = delete;
    AlignedBuf& operator=(const AlignedBuf&) = delete;
};

struct BitmapLock {
    JNIEnv* env;
    jobject bmp;
    void* pixels = nullptr;
    bool locked = false;

    BitmapLock(JNIEnv* e, jobject b) : env(e), bmp(b) {
        locked = AndroidBitmap_lockPixels(env, bmp, &pixels) >= 0;
    }
    ~BitmapLock() { if (locked) AndroidBitmap_unlockPixels(env, bmp); }
    BitmapLock(const BitmapLock&) = delete;
    BitmapLock& operator=(const BitmapLock&) = delete;
};

void throwEx(JNIEnv* env, const char* cls, const char* msg) {
    env->ThrowNew(env->FindClass(cls), msg);
}

bool checkBitmapInfo(JNIEnv* env, jobject bmp, AndroidBitmapInfo& info, const char* tag) {
    if (AndroidBitmap_getInfo(env, bmp, &info) < 0) {
        throwEx(env, "java/lang/RuntimeException", tag);
        return false;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throwEx(env, "java/lang/IllegalArgumentException", "Bitmap must be RGBA_8888");
        return false;
    }
    if (info.width == 0 || info.height == 0) {
        throwEx(env, "java/lang/IllegalArgumentException", "Bitmap dimensions must be nonzero");
        return false;
    }
    return true;
}

// Returns false and throws if the stride value overflows int (i.e. width > 536M pixels).
bool strideFitsInt(JNIEnv* env, size_t stride) {
    if (stride > static_cast<size_t>(INT_MAX)) {
        throwEx(env, "java/lang/IllegalArgumentException", "Bitmap width too large");
        return false;
    }
    return true;
}

// Rounded-up aligned_alloc size. Returns false and throws on overflow.
bool safeAllocSize(JNIEnv* env, size_t raw, size_t& out) {
    if (raw > SIZE_MAX - 63) {
        throwEx(env, "java/lang/OutOfMemoryError", "Bitmap too large to allocate intermediate buffer");
        return false;
    }
    out = (raw + 63) & ~size_t{63};
    return true;
}

} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_libyuvresizer_LibyuvResizerModule_nativeResize(
    JNIEnv* env,
    jobject /* thiz */,
    jobject srcBitmap,
    jobject dstBitmap,
    jint filterMode
) {
    if (filterMode < 0 || filterMode > 3) {
        throwEx(env, "java/lang/IllegalArgumentException", "filterMode must be 0–3");
        return;
    }

    AndroidBitmapInfo srcInfo{}, dstInfo{};
    if (!checkBitmapInfo(env, srcBitmap, srcInfo, "AndroidBitmap_getInfo failed (src)")) return;
    if (!checkBitmapInfo(env, dstBitmap, dstInfo, "AndroidBitmap_getInfo failed (dst)")) return;

    BitmapLock srcLock(env, srcBitmap);
    if (!srcLock.locked) { throwEx(env, "java/lang/RuntimeException", "lockPixels failed (src)"); return; }
    BitmapLock dstLock(env, dstBitmap);
    if (!dstLock.locked) { throwEx(env, "java/lang/RuntimeException", "lockPixels failed (dst)"); return; }

    const int result = libyuv::ARGBScale(
        static_cast<const uint8_t*>(srcLock.pixels), static_cast<int>(srcInfo.stride),
        static_cast<int>(srcInfo.width),  static_cast<int>(srcInfo.height),
        static_cast<uint8_t*>(dstLock.pixels), static_cast<int>(dstInfo.stride),
        static_cast<int>(dstInfo.width),  static_cast<int>(dstInfo.height),
        static_cast<libyuv::FilterModeEnum>(filterMode)
    );
    if (result != 0) throwEx(env, "java/lang/RuntimeException", "ARGBScale failed");
}

extern "C" JNIEXPORT void JNICALL
Java_com_libyuvresizer_LibyuvResizerModule_nativeResizeAndRotate(
    JNIEnv* env,
    jobject /* thiz */,
    jobject srcBitmap,
    jobject dstBitmap,
    jint rotation,
    jint filterMode
) {
    if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
        throwEx(env, "java/lang/IllegalArgumentException", "rotation must be 0, 90, 180, or 270");
        return;
    }
    if (filterMode < 0 || filterMode > 3) {
        throwEx(env, "java/lang/IllegalArgumentException", "filterMode must be 0–3");
        return;
    }

    AndroidBitmapInfo srcInfo{}, dstInfo{};
    if (!checkBitmapInfo(env, srcBitmap, srcInfo, "AndroidBitmap_getInfo failed (src)")) return;
    if (!checkBitmapInfo(env, dstBitmap, dstInfo, "AndroidBitmap_getInfo failed (dst)")) return;

    BitmapLock srcLock(env, srcBitmap);
    if (!srcLock.locked) { throwEx(env, "java/lang/RuntimeException", "lockPixels failed (src)"); return; }
    BitmapLock dstLock(env, dstBitmap);
    if (!dstLock.locked) { throwEx(env, "java/lang/RuntimeException", "lockPixels failed (dst)"); return; }

    auto* src = static_cast<const uint8_t*>(srcLock.pixels);
    auto* dst = static_cast<uint8_t*>(dstLock.pixels);
    const int srcW      = static_cast<int>(srcInfo.width);
    const int srcH      = static_cast<int>(srcInfo.height);
    const int srcStride = static_cast<int>(srcInfo.stride);
    const int dstW      = static_cast<int>(dstInfo.width);
    const int dstH      = static_cast<int>(dstInfo.height);
    const int dstStride = static_cast<int>(dstInfo.stride);
    const auto filter   = static_cast<libyuv::FilterModeEnum>(filterMode);

    libyuv::RotationMode mode = libyuv::kRotate0;
    if (rotation == 90)  mode = libyuv::kRotate90;
    if (rotation == 180) mode = libyuv::kRotate180;
    if (rotation == 270) mode = libyuv::kRotate270;

    // Pure scale — no rotation.
    if (rotation == 0) {
        if (libyuv::ARGBScale(src, srcStride, srcW, srcH, dst, dstStride, dstW, dstH, filter) != 0)
            throwEx(env, "java/lang/RuntimeException", "ARGBScale failed");
        return;
    }

    // 90/270 transpose dims; 180 keeps dims.
    const bool swap = (rotation == 90 || rotation == 270);

    // Choose operation order to minimise the intermediate buffer and the more
    // expensive of the two operations:
    //   downscale  →  scale first (to smaller pre-rotation dims) → rotate
    //   upscale    →  rotate first (on smaller source) → scale
    const size_t srcArea = static_cast<size_t>(srcW) * static_cast<size_t>(srcH);
    const size_t dstArea = static_cast<size_t>(dstW) * static_cast<size_t>(dstH);

    if (dstArea > srcArea) {
        // Upscale path: rotate src (small) → tmp, then scale tmp → dst.
        const int tmpW = swap ? srcH : srcW;
        const int tmpH = swap ? srcW : srcH;
        const size_t tmpStrideRaw = static_cast<size_t>(tmpW) * 4;
        if (!strideFitsInt(env, tmpStrideRaw)) return;

        size_t allocSize = 0;
        if (!safeAllocSize(env, tmpStrideRaw * static_cast<size_t>(tmpH), allocSize)) return;

        AlignedBuf tmp(allocSize);
        if (!tmp.ptr) {
            throwEx(env, "java/lang/OutOfMemoryError", "nativeResizeAndRotate: malloc failed");
            return;
        }

        const int tmpStride = static_cast<int>(tmpStrideRaw);
        if (libyuv::ARGBRotate(src, srcStride, tmp.ptr, tmpStride,
                                srcW, srcH, mode) != 0) {
            throwEx(env, "java/lang/RuntimeException", "ARGBRotate failed"); return;
        }
        if (libyuv::ARGBScale(tmp.ptr, tmpStride, tmpW, tmpH,
                               dst, dstStride, dstW, dstH, filter) != 0)
            throwEx(env, "java/lang/RuntimeException", "ARGBScale failed");
    } else {
        // Downscale path: scale src → pre (pre-rotation dims), then rotate pre → dst.
        const int preW = swap ? dstH : dstW;
        const int preH = swap ? dstW : dstH;
        const size_t preStrideRaw = static_cast<size_t>(preW) * 4;
        if (!strideFitsInt(env, preStrideRaw)) return;

        size_t allocSize = 0;
        if (!safeAllocSize(env, preStrideRaw * static_cast<size_t>(preH), allocSize)) return;

        AlignedBuf pre(allocSize);
        if (!pre.ptr) {
            throwEx(env, "java/lang/OutOfMemoryError", "nativeResizeAndRotate: malloc failed");
            return;
        }

        const int preStride = static_cast<int>(preStrideRaw);
        if (libyuv::ARGBScale(src, srcStride, srcW, srcH, pre.ptr, preStride,
                               preW, preH, filter) != 0) {
            throwEx(env, "java/lang/RuntimeException", "ARGBScale failed"); return;
        }
        if (libyuv::ARGBRotate(pre.ptr, preStride, dst, dstStride,
                                preW, preH, mode) != 0)
            throwEx(env, "java/lang/RuntimeException", "ARGBRotate failed");
    }
}
