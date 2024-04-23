package org.example;

import com.breakfastquay.rubberband.RubberBandStretcher;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String inputFilePath = "D:/MyProjects/RubberBand/RubberBand/voice32.wav";
        String outputFilePath = "D:/MyProjects/RubberBand/RubberBand/output/output32.wav";

        try {
            File inputFile = new File(inputFilePath);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(inputFile);
            AudioFormat format = inputStream.getFormat();


            RubberBandStretcher stretcher = new RubberBandStretcher(
                    (int) inputStream.getFormat().getSampleRate(),
                    inputStream.getFormat().getChannels(),
                            RubberBandStretcher.DefaultOptions ,
                    1.0, 1.0
            );


            int blockSize = 1024 * (format.getSampleSizeInBits()/8)  * (format.getChannels());
            stretcher.setMaxProcessSize(blockSize);
            stretcher.setTimeRatio(2.05);
            stretcher.setPitchScale(1.0);

            File outputFile = new File(outputFilePath);
            processAudioData(inputStream, stretcher, format, outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void processAudioData (AudioInputStream stream, RubberBandStretcher stretcher, AudioFormat format, File outputFile) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[ 1024 * (format.getSampleSizeInBits()/8)  * (format.getChannels())];
        int bytesRead;

        List<float[][]> allSamples = new ArrayList<>();

        while ((bytesRead = stream.read(buffer)) != -1) {

            float[][] samples = convertAudioBytes(buffer, format);
           allSamples.add(samples);
        }
        for (float[][] block : allSamples) {
           stretcher.study(block, 0, block[0].length, false);
        }

        stretcher.study(new float[format.getChannels()][0], 0, 0, true);

        for (float[][] samples : allSamples) {
            stretcher.process(samples, false);
            retrieveAndWriteProcessedData(stretcher, format, byteArrayOutputStream);



        }

        // Finalize processing
        stretcher.process(new float[format.getChannels()][0], true);
        retrieveAndWriteProcessedData(stretcher, format, byteArrayOutputStream);
        stretcher.dispose();

        byte[] audioBytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream outputAIS = new AudioInputStream(bais, format, audioBytes.length / format.getFrameSize());

        AudioSystem.write(outputAIS, AudioFileFormat.Type.WAVE, outputFile);
    }

    private static void retrieveAndWriteProcessedData(RubberBandStretcher stretcher, AudioFormat format, ByteArrayOutputStream bufferedOut) throws IOException {
        int retrieved;
        while ((retrieved = stretcher.available()) > 0) {
            float[][] output = new float[format.getChannels()][retrieved];
            int framesRetrieved = stretcher.retrieve(output, 0, retrieved);
            if (framesRetrieved > 0) {
                if (framesRetrieved < retrieved) {
                    output = trimOutputArray(output, framesRetrieved);
                }
               byte[] bytes = floatsToBytes(output, format);
               bufferedOut.write(bytes);
            }
        }
    }

    private static byte[] floatsToBytes(float[][] samples, AudioFormat format) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(samples[0].length * format.getChannels() * (format.getSampleSizeInBits() / 8));
        byteBuffer.order(format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < samples[0].length; i++) {
            for (int channel = 0; channel < format.getChannels(); channel++) {
                float sample = samples[channel][i];
                switch (format.getSampleSizeInBits()) {
                    case 8:
                        int value8 = (int) (Math.max(-1.0f, Math.min(1.0f, sample)) * 127.0f) + 128;
                        byteBuffer.put((byte) (value8 & 0xFF));
                        break;
                    case 16:
                        short value16 = (short) (Math.max(-1.0f, Math.min(1.0f, sample)) * 32767.0f);
                        byteBuffer.putShort(value16);
                        break;
                    case 24:
                        int value24 = (int) (Math.max(-1.0f, Math.min(1.0f, sample)) * 8388607.0f);
                        byteBuffer.put((byte) (value24 & 0xFF));
                        byteBuffer.put((byte) ((value24 >> 8) & 0xFF));
                        byteBuffer.put((byte) ((value24 >> 16) & 0xFF));
                        break;
                    case 32:
                        if (format.getEncoding().equals(AudioFormat.Encoding.PCM_FLOAT)) {
                            int value32 = (int) (Math.max(-1.0f, Math.min(1.0f, sample)) * 2147483647.0f);
                            byteBuffer.putInt(value32);
                        }
                        break;

                    default:
                        throw new UnsupportedOperationException("Unsupported bit depth: " + format.getSampleSizeInBits());
                }
            }
        }

        return byteBuffer.array();
    }


    private static float[][] convertAudioBytes(byte[] audioBytes, AudioFormat format) {
        int numChannels = format.getChannels();

        switch (format.getSampleSizeInBits()) {
            case 8:
                return convert8BitToFloat(audioBytes, numChannels);
            case 16:
                return convert16BitToFloat(audioBytes, numChannels);

            case 24:
                return convert24BitToFloat(audioBytes, numChannels);
            case 32:
                if (format.getEncoding().equals(AudioFormat.Encoding.PCM_FLOAT)) {
                    return convert32BitFloatToFloat(audioBytes, numChannels,format.isBigEndian());
                }
            default:
                throw new IllegalArgumentException("Unsupported audio bit depth: " + format.getSampleSizeInBits());
        }
    }

    private static float[][] convert16BitToFloat(byte[] audioBytes, int numChannels) {
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
        int numSamples = audioBytes.length / 2;
        float[][] samples = new float[numChannels][numSamples / numChannels];

        for (int i = 0; i < numSamples; i++) {
            short value = buffer.getShort();
            samples[i % numChannels][i / numChannels] = value / 32768.0f;
        }

        return samples;
    }

    private static float[][] convert24BitToFloat(byte[] audioBytes, int numChannels) {
        int numSamplesPerChannel = (audioBytes.length / 3) / numChannels;
        float[][] samples = new float[numChannels][numSamplesPerChannel];

        for (int sampleIndex = 0; sampleIndex < numSamplesPerChannel * numChannels; sampleIndex++) {
            int byteIndex = sampleIndex * 3;
            int lower = audioBytes[byteIndex] & 0xFF;
            int middle = audioBytes[byteIndex + 1] & 0xFF;
            int upper = audioBytes[byteIndex + 2] & 0xFF;
            int value = (upper << 16) | (middle << 8) | lower;

            if (value >= 0x800000) {
                value |= 0xFF000000;
            }

            int channel = sampleIndex % numChannels;
            int sampleNumber = sampleIndex / numChannels;

            samples[channel][sampleNumber] = value / 8388608.0f;
        }

        return samples;
    }

    private static float[][] trimOutputArray(float[][] output, int framesRetrieved) {
        float[][] trimmedOutput = new float[output.length][framesRetrieved];
        for (int channel = 0; channel < output.length; channel++) {
            System.arraycopy(output[channel], 0, trimmedOutput[channel], 0, framesRetrieved);
        }
        return trimmedOutput;
    }

    private static float[][] convert32BitFloatToFloat(byte[] audioBytes, int numChannels, boolean isBigEndian) {
        ByteBuffer buffer = ByteBuffer.wrap(audioBytes);
        buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        int numSamples = audioBytes.length / (4 * numChannels); // 4 bytes per 32-bit float
        float[][] samples = new float[numChannels][numSamples];

        for (int i = 0; i < numSamples; i++) {
            for (int channel = 0; channel < numChannels; channel++) {
                samples[channel][i] = buffer.getFloat();
            }
        }

        return samples;
    }
    private static float[][] convert8BitToFloat(byte[] audioBytes, int numChannels) {
        int numSamples = audioBytes.length / numChannels;
        float[][] samples = new float[numChannels][numSamples];

        for (int i = 0; i < numSamples * numChannels; i++) {
            int channel = i % numChannels;
            int sampleIndex = i / numChannels;
            int value = (audioBytes[i] & 0xFF) - 128;
            samples[channel][sampleIndex] = value / 128.0f;
        }

        return samples;
    }




}
