package ca.ernestwong.multimedia.audio;

public record WavData(
    int sampleRate,
    int bitsPerSample,
    int channels,
    int[][] samples
) {
    public WavData {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive");
        }
        if (bitsPerSample != 8 && bitsPerSample != 16) {
            throw new IllegalArgumentException("Only 8 and 16 bit PCM are supported");
        }
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("Only mono and stereo audio are supported");
        }
        if (samples == null || samples.length != channels) {
            throw new IllegalArgumentException("Sample arrays must match the channel count");
        }
        int frames = samples[0].length;
        for (int[] channel : samples) {
            if (channel.length != frames) {
                throw new IllegalArgumentException("Every channel must have the same number of frames");
            }
        }
    }

    public int frameCount() {
        return samples[0].length;
    }

    public double durationSeconds() {
        return frameCount() / (double) sampleRate;
    }

    public int[] channel(int index) {
        if (index < 0 || index >= channels) {
            throw new IllegalArgumentException("Channel index is out of range");
        }
        return samples[index];
    }
}

