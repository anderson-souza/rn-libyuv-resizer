import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  resize(
    filePath: string,
    targetWidth: number,
    targetHeight: number,
    quality: number,
    rotation: number,
    mode: string,
    outputPath: string,
    filterMode: string,
    scaleConstraint: string
  ): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('LibyuvResizer');
