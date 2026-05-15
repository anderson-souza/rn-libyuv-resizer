#import "LibyuvResizer.h"

@implementation LibyuvResizer
- (NSNumber *)multiply:(double)a b:(double)b {
    NSNumber *result = @(a * b);

    return result;
}

- (void)resize:(NSString *)filePath
   targetWidth:(double)targetWidth
  targetHeight:(double)targetHeight
       quality:(double)quality
      rotation:(double)rotation
          mode:(NSString *)mode
    outputPath:(NSString *)outputPath
    filterMode:(NSString *)filterMode
scaleConstraint:(NSString *)scaleConstraint
       resolve:(RCTPromiseResolveBlock)resolve
        reject:(RCTPromiseRejectBlock)reject
{
    reject(@"E_NOT_IMPLEMENTED", @"resize is not yet implemented on iOS", nil);
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeLibyuvResizerSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"LibyuvResizer";
}

@end
