import type { ResizeOptions } from './multiply.native';

export type { RotationAngle, ResizeOptions } from './multiply.native';

export function resize(
  _filePath: string,
  _targetWidth: number,
  _targetHeight: number,
  _quality: number,
  _options?: ResizeOptions
): Promise<string> {
  return Promise.reject(
    new Error(
      "'react-native-libyuv-resizer' is only supported on native platforms."
    )
  );
}
