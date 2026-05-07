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
