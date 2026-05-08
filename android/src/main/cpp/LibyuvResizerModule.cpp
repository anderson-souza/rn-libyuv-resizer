#include <jni.h>
#include <android/bitmap.h>
#include <cstdlib>
#include "libyuv/scale_argb.h"
#include "libyuv/rotate_argb.h"

namespace {

struct MallocGuard {
    uint8_t* ptr;
    explicit MallocGuard(size_t n) : ptr(static_cast<uint8_t*>(std::malloc(n))) {}
    ~MallocGuard() { std::free(ptr); }
    MallocGuard(const MallocGuard&) = delete;
    MallocGuard& operator=(const MallocGuard&) = delete;
};

} // namespace

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

    if (AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels) < 0) {
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "AndroidBitmap_lockPixels failed (src)");
        return;
    }
    if (AndroidBitmap_lockPixels(env, dstBitmap, &dstPixels) < 0) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "AndroidBitmap_lockPixels failed (dst)");
        return;
    }

    libyuv::ARGBScale(
        static_cast<const uint8_t*>(srcPixels), static_cast<int>(srcInfo.stride),
        static_cast<int>(srcInfo.width),  static_cast<int>(srcInfo.height),
        static_cast<uint8_t*>(dstPixels), static_cast<int>(dstInfo.stride),
        static_cast<int>(dstInfo.width),  static_cast<int>(dstInfo.height),
        libyuv::kFilterBox
    );

    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);
}

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

    // 90/270 rotations swap output dims — pre-rotation buffer has transposed size
    const bool swap = (rotation == 90 || rotation == 270);
    const uint32_t preW = swap ? dstInfo.height : dstInfo.width;
    const uint32_t preH = swap ? dstInfo.width  : dstInfo.height;
    // use size_t to avoid uint32_t overflow on large images
    const size_t preStride  = static_cast<size_t>(preW) * 4;
    const size_t allocSize  = preStride * static_cast<size_t>(preH);

    MallocGuard preBuf(allocSize);
    if (!preBuf.ptr) {
        jclass ex = env->FindClass("java/lang/OutOfMemoryError");
        env->ThrowNew(ex, "nativeResizeAndRotate: malloc failed");
        return;
    }

    void* srcPixels = nullptr;
    void* dstPixels = nullptr;

    if (AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels) < 0) {
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "AndroidBitmap_lockPixels failed (src)");
        return;  // preBuf freed by MallocGuard dtor
    }
    if (AndroidBitmap_lockPixels(env, dstBitmap, &dstPixels) < 0) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "AndroidBitmap_lockPixels failed (dst)");
        return;  // preBuf freed by MallocGuard dtor
    }

    const int scaleResult = libyuv::ARGBScale(
        static_cast<const uint8_t*>(srcPixels), static_cast<int>(srcInfo.stride),
        static_cast<int>(srcInfo.width), static_cast<int>(srcInfo.height),
        preBuf.ptr, static_cast<int>(preStride),
        static_cast<int>(preW), static_cast<int>(preH),
        libyuv::kFilterBox
    );

    if (scaleResult != 0) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        AndroidBitmap_unlockPixels(env, dstBitmap);
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "ARGBScale failed");
        return;
    }

    libyuv::RotationMode mode = libyuv::kRotate0;
    if (rotation == 90)  mode = libyuv::kRotate90;
    if (rotation == 180) mode = libyuv::kRotate180;
    if (rotation == 270) mode = libyuv::kRotate270;

    const int rotateResult = libyuv::ARGBRotate(
        preBuf.ptr, static_cast<int>(preStride),
        static_cast<uint8_t*>(dstPixels), static_cast<int>(dstInfo.stride),
        static_cast<int>(preW), static_cast<int>(preH),
        mode
    );

    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, dstBitmap);

    if (rotateResult != 0) {
        jclass ex = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(ex, "ARGBRotate failed");
    }
    // MallocGuard frees preBuf on scope exit
}
