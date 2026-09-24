package ca.ernestwong.multimedia.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class FourierTransform {
    private static final int MAX_DIMENSION = 256;

    private FourierTransform() {
    }

    public static Result spectrum(BufferedImage source) {
        int width = powerOfTwoAtMost(Math.min(MAX_DIMENSION, source.getWidth()));
        int height = powerOfTwoAtMost(Math.min(MAX_DIMENSION, source.getHeight()));
        BufferedImage scaled = resize(source, width, height);
        double[][] real = new double[height][width];
        double[][] imaginary = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = ImageTransforms.luminance(scaled.getRGB(x, y));
                real[y][x] = ((x + y) & 1) == 0 ? value : -value;
            }
        }

        for (int y = 0; y < height; y++) {
            fft(real[y], imaginary[y]);
        }
        double[] columnReal = new double[height];
        double[] columnImaginary = new double[height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                columnReal[y] = real[y][x];
                columnImaginary[y] = imaginary[y][x];
            }
            fft(columnReal, columnImaginary);
            for (int y = 0; y < height; y++) {
                real[y][x] = columnReal[y];
                imaginary[y][x] = columnImaginary[y];
            }
        }

        double[][] magnitude = new double[height][width];
        double maximum = 0.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                magnitude[y][x] = Math.log1p(Math.hypot(real[y][x], imaginary[y][x]));
                maximum = Math.max(maximum, magnitude[y][x]);
            }
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = maximum == 0.0
                    ? 0
                    : ImageTransforms.clamp((int) Math.round(magnitude[y][x] * 255.0 / maximum));
                int rgb = (value << 16) | (value << 8) | value;
                output.setRGB(x, y, rgb);
            }
        }
        return new Result(output, width, height);
    }

    private static void fft(double[] real, double[] imaginary) {
        int length = real.length;
        if (length != imaginary.length || Integer.bitCount(length) != 1) {
            throw new IllegalArgumentException("FFT arrays must have matching power of two lengths");
        }

        for (int index = 1, reversed = 0; index < length; index++) {
            int bit = length >>> 1;
            while ((reversed & bit) != 0) {
                reversed ^= bit;
                bit >>>= 1;
            }
            reversed ^= bit;
            if (index < reversed) {
                swap(real, index, reversed);
                swap(imaginary, index, reversed);
            }
        }

        for (int size = 2; size <= length; size <<= 1) {
            double angle = -2.0 * Math.PI / size;
            double phaseStepReal = Math.cos(angle);
            double phaseStepImaginary = Math.sin(angle);
            for (int start = 0; start < length; start += size) {
                double phaseReal = 1.0;
                double phaseImaginary = 0.0;
                int half = size / 2;
                for (int offset = 0; offset < half; offset++) {
                    int even = start + offset;
                    int odd = even + half;
                    double oddReal = real[odd] * phaseReal - imaginary[odd] * phaseImaginary;
                    double oddImaginary = real[odd] * phaseImaginary + imaginary[odd] * phaseReal;
                    real[odd] = real[even] - oddReal;
                    imaginary[odd] = imaginary[even] - oddImaginary;
                    real[even] += oddReal;
                    imaginary[even] += oddImaginary;

                    double nextPhaseReal = phaseReal * phaseStepReal - phaseImaginary * phaseStepImaginary;
                    phaseImaginary = phaseReal * phaseStepImaginary + phaseImaginary * phaseStepReal;
                    phaseReal = nextPhaseReal;
                }
            }
        }
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return output;
    }

    private static int powerOfTwoAtMost(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        int result = 1;
        while (result <= value / 2) {
            result *= 2;
        }
        return result;
    }

    private static void swap(double[] values, int first, int second) {
        double temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    public record Result(BufferedImage image, int width, int height) {
    }
}
