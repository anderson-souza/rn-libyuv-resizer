import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  resize(
    filePath: string,
    targetWidth: number,
    targetHeight: number,
    quality: number
  ): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer');
