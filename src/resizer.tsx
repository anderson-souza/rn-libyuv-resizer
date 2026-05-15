import type { ResizeOptions } from './resizer.native';

export type {
  RotationAngle,
  ResizeMode,
  ResizeOptions,
  ScaleConstraint,
} from './resizer.native';

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
