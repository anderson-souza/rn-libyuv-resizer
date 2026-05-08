import LibyuvResizer from './NativeLibyuvResizer';

export type RotationAngle = 0 | 90 | 180 | 270 | -90 | -180 | -270;

export interface ResizeOptions {
  rotation?: RotationAngle;
}

function toCanonicalAngle(angle: RotationAngle): 0 | 90 | 180 | 270 {
  return (((angle % 360) + 360) % 360) as 0 | 90 | 180 | 270;
}

export function resize(
  filePath: string,
  targetWidth: number,
  targetHeight: number,
  quality: number,
  options?: ResizeOptions
): Promise<string> {
  const rotation =
    options?.rotation != null ? toCanonicalAngle(options.rotation) : 0;
  return LibyuvResizer.resize(
    filePath,
    targetWidth,
    targetHeight,
    quality,
    rotation
  );
}
