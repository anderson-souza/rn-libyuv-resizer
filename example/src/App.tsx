import { useState } from 'react';
import {
  ActivityIndicator,
  Button,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';
import { resize } from 'react-native-libyuv-resizer';

type ResizeResult = {
  outputPath: string;
  inputSize: { width: number; height: number };
  elapsedMs: number;
};

export default function App() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ResizeResult | null>(null);

  async function pickAndResize() {
    setError(null);
    setResult(null);

    const response = await launchImageLibrary({
      mediaType: 'photo',
      includeBase64: false,
    });
    if (response.didCancel || !response.assets?.[0]?.uri) return;

    const asset = response.assets[0];
    const filePath = asset.uri!.replace('file://', '');

    setLoading(true);
    try {
      const start = Date.now();
      const outputPath = await resize(filePath, 320, 240, 80);
      const elapsedMs = Date.now() - start;
      setResult({
        outputPath,
        inputSize: { width: asset.width ?? 0, height: asset.height ?? 0 },
        elapsedMs,
      });
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>libyuv Resizer</Text>
      <Button title="Pick image from gallery" onPress={pickAndResize} />

      {loading && <ActivityIndicator style={styles.gap} size="large" />}

      {error && <Text style={styles.error}>{error}</Text>}

      {result && (
        <View style={styles.gap}>
          <Text style={styles.label}>
            Input: {result.inputSize.width}×{result.inputSize.height}
          </Text>
          <Text style={styles.label}>Output: 320×240</Text>
          <Text style={styles.label}>Time: {result.elapsedMs}ms</Text>
          <Text style={styles.label} numberOfLines={2}>
            Path: {result.outputPath}
          </Text>
          <Image
            source={{ uri: `file://${result.outputPath}` }}
            style={styles.preview}
            resizeMode="contain"
          />
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 24,
  },
  gap: {
    marginTop: 24,
    alignItems: 'center',
  },
  label: {
    fontSize: 13,
    marginBottom: 4,
    textAlign: 'center',
  },
  error: {
    marginTop: 16,
    color: 'red',
    textAlign: 'center',
  },
  preview: {
    marginTop: 12,
    width: 320,
    height: 240,
    borderWidth: 1,
    borderColor: '#ccc',
  },
});
