import LibyuvResizer from './NativeLibyuvResizer';

export type RotationAngle = 0 | 90 | 180 | 270 | -90 | -180 | -270;
export type ResizeMode = 'contain' | 'cover' | 'stretch';
export type FilterMode = 'none' | 'linear' | 'bilinear' | 'box';

export interface ResizeOptions {
  rotation?: RotationAngle;
  mode?: ResizeMode;
  outputPath?: string;
  filterMode?: FilterMode;
}

const VALID_MODES: ResizeMode[] = ['contain', 'cover', 'stretch'];
const VALID_FILTER_MODES: FilterMode[] = ['none', 'linear', 'bilinear', 'box'];

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
  const mode: ResizeMode = options?.mode ?? 'contain';
  if (!VALID_MODES.includes(mode)) {
    return Promise.reject(new TypeError(`Invalid resize mode: '${mode}'`));
  }
  const filterMode: FilterMode = options?.filterMode ?? 'box';
  if (!VALID_FILTER_MODES.includes(filterMode)) {
    return Promise.reject(
      new TypeError(`Invalid filter mode: '${filterMode}'`)
    );
  }
  return LibyuvResizer.resize(
    filePath,
    targetWidth,
    targetHeight,
    quality,
    rotation,
    mode,
    options?.outputPath ?? '',
    filterMode
  );
}
