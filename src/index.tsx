/**
 * `react-native-libyuv-resizer` — high-performance image resizer for React
 * Native backed by libyuv on Android.
 *
 * **Entry point.** Import {@link resize} and the supporting types from here.
 *
 * @example
 * ```ts
 * import { resize } from 'react-native-libyuv-resizer';
 *
 * const result = await resize('/path/to/photo.jpg', 1280, 720, 85);
 * console.log(result.path, result.width, result.height);
 * ```
 *
 * @packageDocumentation
 */
export { resize } from './resizer';
export type {
  RotationAngle,
  ResizeMode,
  ResizeOptions,
  ResizeResult,
} from './resizer';
