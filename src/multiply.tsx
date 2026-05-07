export function resize(
  _filePath: string,
  _targetWidth: number,
  _targetHeight: number,
  _quality: number
): Promise<string> {
  return Promise.reject(
    new Error(
      "'react-native-libyuv-resizer' is only supported on native platforms."
    )
  );
}
