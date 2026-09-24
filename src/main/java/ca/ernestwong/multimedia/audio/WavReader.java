package ca.ernestwong.multimedia.audio;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class WavReader {
    private WavReader() {
    }

    public static WavData read(Path path) throws IOException {
        try (RandomAccessFile input = new RandomAccessFile(path.toFile(), "r")) {
            if (!"RIFF".equals(readFourCc(input))) {
                throw new IOException("The file does not begin with a RIFF header");
            }
            readUnsignedIntLittleEndian(input);
            if (!"WAVE".equals(readFourCc(input))) {
                throw new IOException("The RIFF file is not a WAVE file");
            }

            Format format = null;
            long dataOffset = -1;
            long dataSize = -1;

            while (input.getFilePointer() + 8 <= input.length()) {
                String chunkId = readFourCc(input);
                long chunkSize = readUnsignedIntLittleEndian(input);
                long chunkStart = input.getFilePointer();
                long nextChunk = chunkStart + chunkSize + (chunkSize & 1L);
                if (nextChunk > input.length() + 1) {
                    throw new IOException("WAV chunk extends beyond the end of the file");
                }

                if ("fmt ".equals(chunkId)) {
                    format = readFormat(input, chunkSize);
                } else if ("data".equals(chunkId)) {
                    dataOffset = chunkStart;
                    dataSize = chunkSize;
                }
                input.seek(Math.min(nextChunk, input.length()));
            }

            if (format == null) {
                throw new IOException("The WAV file has no format chunk");
            }
            if (dataOffset < 0 || dataSize < 0) {
                throw new IOException("The WAV file has no data chunk");
            }
            return readSamples(input, format, dataOffset, dataSize);
        }
    }

    private static Format readFormat(RandomAccessFile input, long chunkSize) throws IOException {
        if (chunkSize < 16) {
            throw new IOException("The WAV format chunk is too short");
        }
        int encoding = readUnsignedShortLittleEndian(input);
        int channels = readUnsignedShortLittleEndian(input);
        long sampleRate = readUnsignedIntLittleEndian(input);
        readUnsignedIntLittleEndian(input);
        int blockAlign = readUnsignedShortLittleEndian(input);
        int bitsPerSample = readUnsignedShortLittleEndian(input);

        if (encoding != 1) {
            throw new IOException("Only uncompressed PCM WAV files are supported");
        }
        if (channels < 1 || channels > 2) {
            throw new IOException("Only mono and stereo WAV files are supported");
        }
        if (bitsPerSample != 8 && bitsPerSample != 16) {
            throw new IOException("Only 8 and 16 bit PCM WAV files are supported");
        }
        int expectedBlockAlign = channels * (bitsPerSample / 8);
        if (blockAlign != expectedBlockAlign) {
            throw new IOException("The WAV block alignment is inconsistent with its format");
        }
        if (sampleRate <= 0 || sampleRate > Integer.MAX_VALUE) {
            throw new IOException("The WAV sample rate is invalid");
        }
        return new Format((int) sampleRate, bitsPerSample, channels, blockAlign);
    }

    private static WavData readSamples(
        RandomAccessFile input,
        Format format,
        long dataOffset,
        long dataSize
    ) throws IOException {
        long frameCountLong = dataSize / format.blockAlign();
        if (frameCountLong > Integer.MAX_VALUE) {
            throw new IOException("The WAV file is too large to open in memory");
        }
        int frameCount = (int) frameCountLong;
        int[][] samples = new int[format.channels()][frameCount];

        input.seek(dataOffset);
        for (int frame = 0; frame < frameCount; frame++) {
            for (int channel = 0; channel < format.channels(); channel++) {
                if (format.bitsPerSample() == 8) {
                    samples[channel][frame] = input.readUnsignedByte() - 128;
                } else {
                    int low = input.readUnsignedByte();
                    int high = input.readByte();
                    samples[channel][frame] = (short) ((high << 8) | low);
                }
            }
        }
        return new WavData(
            format.sampleRate(),
            format.bitsPerSample(),
            format.channels(),
            samples
        );
    }

    private static String readFourCc(RandomAccessFile input) throws IOException {
        byte[] bytes = new byte[4];
        int count = input.read(bytes);
        if (count != bytes.length) {
            throw new EOFException("Unexpected end of file while reading a WAV chunk name");
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static int readUnsignedShortLittleEndian(RandomAccessFile input) throws IOException {
        int low = input.readUnsignedByte();
        int high = input.readUnsignedByte();
        return low | (high << 8);
    }

    private static long readUnsignedIntLittleEndian(RandomAccessFile input) throws IOException {
        long first = input.readUnsignedByte();
        long second = input.readUnsignedByte();
        long third = input.readUnsignedByte();
        long fourth = input.readUnsignedByte();
        return first | (second << 8) | (third << 16) | (fourth << 24);
    }

    private record Format(int sampleRate, int bitsPerSample, int channels, int blockAlign) {
    }
}

