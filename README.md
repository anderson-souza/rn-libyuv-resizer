# react-native-libyuv-resizer

High-performance image resizer for React Native using libyuv (Android). Requires **New Architecture** (Turbo Modules).

## Installation

```sh
yarn add react-native-libyuv-resizer
```

> **New Arch only.** Legacy bridge is not supported.

## API

### `resize(filePath, targetWidth, targetHeight, quality, options?): Promise<string>`

| Parameter              | Type            | Default     | Description                                                                        |
| ---------------------- | --------------- | ----------- | ---------------------------------------------------------------------------------- |
| `filePath`             | `string`        | —           | Absolute path to source image                                                      |
| `targetWidth`          | `number`        | —           | Output width in pixels                                                             |
| `targetHeight`         | `number`        | —           | Output height in pixels                                                            |
| `quality`              | `number`        | —           | JPEG quality `0–100`                                                               |
| `options.rotation`     | `RotationAngle` | `0`         | Rotation applied before resize: `0 \| 90 \| 180 \| 270 \| -90 \| -180 \| -270`  |
| `options.mode`         | `ResizeMode`    | `'contain'` | How the image fits the target box: `'contain' \| 'cover' \| 'stretch'`            |
| `options.filterMode`   | `FilterMode`    | `'box'`     | Scaling filter: `'none' \| 'linear' \| 'bilinear' \| 'box'`                      |
| `options.outputPath`   | `string`        | auto        | Absolute path for the output file. Auto-generated if omitted.                     |

Returns a `Promise<string>` — absolute path to the resized image.

### Types

```ts
type RotationAngle = 0 | 90 | 180 | 270 | -90 | -180 | -270;
type ResizeMode = 'contain' | 'cover' | 'stretch';
type FilterMode = 'none' | 'linear' | 'bilinear' | 'box';

interface ResizeOptions {
  rotation?: RotationAngle;
  mode?: ResizeMode;
  filterMode?: FilterMode;
  outputPath?: string;
}
```

### Resize modes

| Mode        | Behavior                                                              |
| ----------- | --------------------------------------------------------------------- |
| `contain`   | Fits entirely within the target box, preserving aspect ratio          |
| `cover`     | Fills the target box and crops excess, preserving aspect ratio        |
| `stretch`   | Stretches to exact target dimensions, ignoring aspect ratio           |

### Filter modes

| Mode        | Quality / Speed trade-off                                             |
| ----------- | --------------------------------------------------------------------- |
| `none`      | Nearest-neighbor — fastest, lowest quality                            |
| `linear`    | Linear interpolation                                                  |
| `bilinear`  | Bilinear interpolation                                                |
| `box`       | Box filter — best quality for downscaling *(default)*                 |

## Usage

```ts
import { resize } from 'react-native-libyuv-resizer';

// Basic resize
const outputPath = await resize('/path/to/photo.jpg', 1280, 720, 85);

// Resize with options
const outputPath = await resize('/path/to/photo.jpg', 1280, 720, 85, {
  rotation: 90,
  mode: 'cover',
  filterMode: 'bilinear',
});

// Custom output path
const outputPath = await resize('/path/to/photo.jpg', 800, 600, 80, {
  outputPath: '/path/to/output.jpg',
});
```

### With react-native-image-picker

```ts
import { launchImageLibrary } from 'react-native-image-picker';
import { resize } from 'react-native-libyuv-resizer';

const result = await launchImageLibrary({ mediaType: 'photo' });
const asset = result.assets?.[0];

if (asset?.uri) {
  const resized = await resize(asset.uri, 800, 600, 80, { mode: 'cover' });
  console.log('Resized image at:', resized);
}
```

## Platform notes

| Platform    | Backend                      |
| ----------- | ---------------------------- |
| Android     | libyuv (`ARGBScale`) via NDK |
| iOS         | Not implemented              |
| Web / other | Throws — native only         |

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT
