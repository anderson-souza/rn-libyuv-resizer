# react-native-libyuv-resizer

High-performance image resizer for React Native using libyuv (Android). Requires **New Architecture** (Turbo Modules).

## Installation

```sh
yarn add react-native-libyuv-resizer
```

> **New Arch only.** Legacy bridge is not supported.

## API

### `resize(filePath, targetWidth, targetHeight, quality, options?): Promise<string>`

| Parameter          | Type            | Description                                                                     |
| ------------------ | --------------- | ------------------------------------------------------------------------------- |
| `filePath`         | `string`        | Absolute path to source image                                                   |
| `targetWidth`      | `number`        | Output width in pixels                                                          |
| `targetHeight`     | `number`        | Output height in pixels                                                         |
| `quality`          | `number`        | JPEG quality `0–100`                                                            |
| `options.rotation` | `RotationAngle` | Optional rotation: `0 \| 90 \| 180 \| 270 \| -90 \| -180 \| -270` (default `0`) |

Returns a `Promise<string>` — absolute path to the resized image.

### Types

```ts
type RotationAngle = 0 | 90 | 180 | 270 | -90 | -180 | -270;

interface ResizeOptions {
  rotation?: RotationAngle;
}
```

## Usage

```ts
import { resize } from 'react-native-libyuv-resizer';

// Basic resize
const outputPath = await resize('/path/to/photo.jpg', 1280, 720, 85);

// Resize with rotation
const outputPath = await resize('/path/to/photo.jpg', 1280, 720, 85, {
  rotation: 90,
});
```

### With react-native-image-picker

```ts
import { launchImageLibrary } from 'react-native-image-picker';
import { resize } from 'react-native-libyuv-resizer';

const result = await launchImageLibrary({ mediaType: 'photo' });
const asset = result.assets?.[0];

if (asset?.uri) {
  const resized = await resize(asset.uri, 800, 600, 80);
  console.log('Resized image at:', resized);
}
```

## Platform notes

| Platform    | Backend                      |
| ----------- | ---------------------------- |
| Android     | libyuv (`ARGBScale`) via NDK |
| iOS         | Not Implemented              |
| Web / other | Throws — native only         |

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT
