import { TurboModuleRegistry, type TurboModule } from 'react-native';

/**
 * Turbo Module contract for the native `LibyuvResizer` implementation.
 *
 * **Do not call this interface directly.** Use the public {@link resize}
 * function from `./resizer` (or the package root) instead — it validates
 * arguments and applies sensible defaults before forwarding to this bridge.
 *
 * This interface is consumed by React Native codegen to auto-generate the
 * JSI glue code (`LibyuvResizerSpec`).
 */
export interface Spec extends TurboModule {
  /**
   * Low-level bridge method. Parameter order mirrors the public `resize()`
   * signature with defaults already resolved.
   *
   * @param filePath    - Absolute path to the source image.
   * @param targetWidth - Output width in pixels.
   * @param targetHeight - Output height in pixels.
   * @param quality     - JPEG quality `0–100`.
   * @param rotation    - Canonical rotation in degrees (`0 | 90 | 180 | 270`).
   * @param mode        - Resize mode string (`'contain' | 'cover' | 'stretch'`).
   * @param outputPath  - Absolute output path, or empty string for auto.
   * @param filterMode  - Scaling filter (`'none' | 'linear' | 'bilinear' | 'box'`).
   * @returns Absolute path of the resized image.
   */
  resize(
    filePath: string,
    targetWidth: number,
    targetHeight: number,
    quality: number,
    rotation: number,
    mode: string,
    outputPath: string,
    filterMode: string
  ): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer');
