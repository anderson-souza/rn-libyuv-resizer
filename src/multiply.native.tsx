import LibyuvResizer from './NativeLibyuvResizer';

export function resize(
  filePath: string,
  targetWidth: number,
  targetHeight: number,
  quality: number
): Promise<string> {
  return LibyuvResizer.resize(filePath, targetWidth, targetHeight, quality);
}
