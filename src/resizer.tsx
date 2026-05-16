import type { ResizeOptions, ResizeResult } from './resizer.native';

export type {
  RotationAngle,
  ResizeMode,
  ResizeOptions,
  ResizeResult,
} from './resizer.native';

/**
 * Web / non-native fallback for {@link resize}.
 *
 * This module is selected by Metro on platforms other than Android/iOS.
 * It always rejects because `react-native-libyuv-resizer` requires a native
 * runtime. Import the real implementation via the `.native` platform extension.
 *
 * @throws {Error} Always — native platform required.
 */
export function resize(
  _filePath: string,
  _targetWidth: number,
  _targetHeight: number,
  _quality: number,
  _options?: ResizeOptions
): Promise<ResizeResult> {
  return Promise.reject(
    new Error(
      "'react-native-libyuv-resizer' is only supported on native platforms."
    )
  );
}
