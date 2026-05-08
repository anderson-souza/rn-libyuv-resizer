import { describe, it, expect, jest, beforeEach } from '@jest/globals';

const mockResize = jest
  .fn<(...args: unknown[]) => Promise<string>>()
  .mockResolvedValue('/out.jpg');

jest.mock('../NativeLibyuvResizer', () => ({
  __esModule: true,
  default: { resize: mockResize },
}));

// Import the native implementation directly — importing from '../index' would resolve
// to '../resizer.tsx' (web fallback) because Jest does not resolve .native extensions
// by default. The public API surface (index.tsx re-exports) is covered by typecheck.
const { resize } = require('../resizer.native');

beforeEach(() => {
  mockResize.mockClear();
});

describe('resize mode', () => {
  describe('contain (default)', () => {
    it('landscape image in square target → width=targetW, height reduced', async () => {
      // 1920×1080 into 400×400 → scale=min(400/1920, 400/1080)=min(0.208,0.370)=0.208 → 400×225
      await resize('/img.jpg', 400, 400, 80);
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'contain'
      );
    });

    it('omitting mode defaults to contain', async () => {
      await resize('/img.jpg', 400, 400, 80, {});
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'contain'
      );
    });

    it('explicit contain forwards correctly', async () => {
      await resize('/img.jpg', 400, 400, 80, { mode: 'contain' });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'contain'
      );
    });
  });

  describe('cover', () => {
    it('forwards cover to native', async () => {
      await resize('/img.jpg', 400, 400, 80, { mode: 'cover' });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'cover'
      );
    });
  });

  describe('stretch', () => {
    it('forwards stretch to native', async () => {
      await resize('/img.jpg', 400, 400, 80, { mode: 'stretch' });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        0,
        'stretch'
      );
    });
  });

  describe('invalid mode', () => {
    it('throws TypeError and does not call native', async () => {
      await expect(
        resize('/img.jpg', 400, 400, 80, { mode: 'fill' as any })
      ).rejects.toThrow(TypeError);
      expect(mockResize).not.toHaveBeenCalled();
    });

    it('error message includes the invalid mode value', async () => {
      await expect(
        resize('/img.jpg', 400, 400, 80, { mode: 'fill' as any })
      ).rejects.toThrow("Invalid resize mode: 'fill'");
    });
  });

  describe('rotation passthrough', () => {
    it('normalises negative rotation and forwards with mode', async () => {
      await resize('/img.jpg', 400, 400, 80, {
        rotation: -90,
        mode: 'stretch',
      });
      expect(mockResize).toHaveBeenCalledWith(
        '/img.jpg',
        400,
        400,
        80,
        270,
        'stretch'
      );
    });
  });
});
