import {
  describe,
  it,
  expect,
  jest,
  beforeEach,
  afterEach,
} from '@jest/globals';

describe('NativeLibyuvResizer arch detection', () => {
  beforeEach(() => {
    jest.resetModules();
  });

  afterEach(() => {
    delete (globalThis as Record<string, unknown>).__turboModuleProxy;
  });

  it('uses TurboModuleRegistry when __turboModuleProxy is set', () => {
    (globalThis as Record<string, unknown>).__turboModuleProxy = {};
    jest.doMock('react-native', () => ({
      TurboModuleRegistry: {
        getEnforcing: jest.fn().mockReturnValue({ resize: jest.fn() }),
      },
      NativeModules: {},
    }));
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    require('../NativeLibyuvResizer');
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const rn = require('react-native') as {
      TurboModuleRegistry: { getEnforcing: ReturnType<typeof jest.fn> };
    };
    expect(rn.TurboModuleRegistry.getEnforcing).toHaveBeenCalledWith(
      'LibyuvResizer'
    );
  });

  it('uses NativeModules when __turboModuleProxy is null', () => {
    (globalThis as Record<string, unknown>).__turboModuleProxy = null;
    jest.doMock('react-native', () => ({
      TurboModuleRegistry: { getEnforcing: jest.fn() },
      NativeModules: { LibyuvResizer: { resize: jest.fn() } },
    }));
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const mod = (require('../NativeLibyuvResizer') as { default: unknown })
      .default;
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const rn = require('react-native') as {
      TurboModuleRegistry: { getEnforcing: ReturnType<typeof jest.fn> };
      NativeModules: { LibyuvResizer: unknown };
    };
    expect(rn.TurboModuleRegistry.getEnforcing).not.toHaveBeenCalled();
    expect(mod).toBe(rn.NativeModules.LibyuvResizer);
  });

  it('throws when NativeModules.LibyuvResizer is missing on legacy arch', () => {
    (globalThis as Record<string, unknown>).__turboModuleProxy = null;
    jest.doMock('react-native', () => ({
      TurboModuleRegistry: { getEnforcing: jest.fn() },
      NativeModules: {},
    }));
    expect(() => require('../NativeLibyuvResizer')).toThrow(
      'react-native-libyuv-resizer: native module not found'
    );
  });
});
