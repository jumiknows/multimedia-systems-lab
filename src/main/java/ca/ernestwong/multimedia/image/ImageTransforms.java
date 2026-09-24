package ca.ernestwong.multimedia.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class ImageTransforms {
    private static final int[][] BAYER_FOUR = {
        {0, 8, 2, 10},
        {12, 4, 14, 6},
        {3, 11, 1, 9},
        {15, 7, 13, 5}
    };

    private ImageTransforms() {
    }

    public static BufferedImage copy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    public static BufferedImage grayscale(BufferedImage source) {
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int value = luminance(source.getRGB(x, y));
                output.setRGB(x, y, gray(value));
            }
        }
        return output;
    }

    public static BufferedImage adjustYuvBrightness(BufferedImage source, int lumaOffset) {
        return transformYuv(source, lumaOffset, 1.0);
    }

    public static BufferedImage adjustYuvSaturation(BufferedImage source, double saturationScale) {
        if (saturationScale < 0.0) {
            throw new IllegalArgumentException("Saturation scale cannot be negative");
        }
        return transformYuv(source, 0.0, saturationScale);
    }

    public static BufferedImage colorizeMonochrome(
        BufferedImage source,
        Color shadowColor,
        Color highlightColor,
        double strength
    ) {
        if (strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("Colourization strength must be between zero and one");
        }
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int value = luminance(source.getRGB(x, y));
                double position = value / 255.0;
                double eased = position * position * (3.0 - 2.0 * position);
                int red = blend(value, interpolate(shadowColor.getRed(), highlightColor.getRed(), eased), strength);
                int green = blend(
                    value,
                    interpolate(shadowColor.getGreen(), highlightColor.getGreen(), eased),
                    strength
                );
                int blue = blend(value, interpolate(shadowColor.getBlue(), highlightColor.getBlue(), eased), strength);
                output.setRGB(x, y, rgb(red, green, blue));
            }
        }
        return output;
    }

    public static BufferedImage orderedDither(BufferedImage source) {
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int value = luminance(source.getRGB(x, y));
                double threshold = (BAYER_FOUR[y % 4][x % 4] + 0.5) * 255.0 / 16.0;
                output.setRGB(x, y, value >= threshold ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }
        return output;
    }

    public static BufferedImage floydSteinbergDither(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double[][] values = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                values[y][x] = luminance(source.getRGB(x, y));
            }
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double oldValue = values[y][x];
                int newValue = oldValue < 128.0 ? 0 : 255;
                output.setRGB(x, y, gray(newValue));
                double error = oldValue - newValue;
                addError(values, x + 1, y, error * 7.0 / 16.0);
                addError(values, x - 1, y + 1, error * 3.0 / 16.0);
                addError(values, x, y + 1, error * 5.0 / 16.0);
                addError(values, x + 1, y + 1, error / 16.0);
            }
        }
        return output;
    }

    public static double psnr(BufferedImage original, BufferedImage processed) {
        if (original.getWidth() != processed.getWidth() || original.getHeight() != processed.getHeight()) {
            throw new IllegalArgumentException("Images must have matching dimensions");
        }
        double squaredError = 0.0;
        long values = (long) original.getWidth() * original.getHeight() * 3L;
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int first = original.getRGB(x, y);
                int second = processed.getRGB(x, y);
                for (int shift = 0; shift <= 16; shift += 8) {
                    int difference = ((first >>> shift) & 0xff) - ((second >>> shift) & 0xff);
                    squaredError += difference * difference;
                }
            }
        }
        double meanSquaredError = squaredError / values;
        if (meanSquaredError == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        return 10.0 * Math.log10(255.0 * 255.0 / meanSquaredError);
    }

    public static int luminance(int rgb) {
        int red = (rgb >>> 16) & 0xff;
        int green = (rgb >>> 8) & 0xff;
        int blue = rgb & 0xff;
        return clamp((int) Math.round(0.2126 * red + 0.7152 * green + 0.0722 * blue));
    }

    public static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int gray(int value) {
        int clipped = clamp(value);
        return (clipped << 16) | (clipped << 8) | clipped;
    }

    private static BufferedImage transformYuv(BufferedImage source, double lumaOffset, double saturationScale) {
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int packed = source.getRGB(x, y);
                double red = (packed >>> 16) & 0xff;
                double green = (packed >>> 8) & 0xff;
                double blue = packed & 0xff;

                double luma = 0.299 * red + 0.587 * green + 0.114 * blue;
                double u = -0.168736 * red - 0.331264 * green + 0.5 * blue;
                double v = 0.5 * red - 0.418688 * green - 0.081312 * blue;

                luma = Math.max(0.0, Math.min(255.0, luma + lumaOffset));
                u *= saturationScale;
                v *= saturationScale;

                int transformedRed = clamp((int) Math.round(luma + 1.402 * v));
                int transformedGreen = clamp((int) Math.round(luma - 0.344136 * u - 0.714136 * v));
                int transformedBlue = clamp((int) Math.round(luma + 1.772 * u));
                output.setRGB(x, y, rgb(transformedRed, transformedGreen, transformedBlue));
            }
        }
        return output;
    }

    private static int interpolate(int start, int end, double position) {
        return clamp((int) Math.round(start + (end - start) * position));
    }

    private static int blend(int gray, int colour, double strength) {
        return clamp((int) Math.round(gray * (1.0 - strength) + colour * strength));
    }

    private static int rgb(int red, int green, int blue) {
        return (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static void addError(double[][] values, int x, int y, double error) {
        if (y >= 0 && y < values.length && x >= 0 && x < values[0].length) {
            values[y][x] = Math.max(0.0, Math.min(255.0, values[y][x] + error));
        }
    }
}
