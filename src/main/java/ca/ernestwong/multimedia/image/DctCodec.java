package ca.ernestwong.multimedia.image;

import java.awt.image.BufferedImage;

public final class DctCodec {
    private static final int BLOCK_SIZE = 8;
    private static final int[][] BASE_QUANTIZATION = {
        {16, 11, 10, 16, 24, 40, 51, 61},
        {12, 12, 14, 19, 26, 58, 60, 55},
        {14, 13, 16, 24, 40, 57, 69, 56},
        {14, 17, 22, 29, 51, 87, 80, 62},
        {18, 22, 37, 56, 68, 109, 103, 77},
        {24, 35, 55, 64, 81, 104, 113, 92},
        {49, 64, 78, 87, 103, 121, 120, 101},
        {72, 92, 95, 98, 112, 100, 103, 99}
    };
    private static final double[][] COSINE = createCosineTable();

    private DctCodec() {
    }

    public static Result compress(BufferedImage source, int quality) {
        int safeQuality = Math.max(1, Math.min(100, quality));
        int[][] quantization = scaledQuantization(safeQuality);
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        long nonZeroCoefficients = 0;
        long totalCoefficients = 0;

        for (int blockY = 0; blockY < source.getHeight(); blockY += BLOCK_SIZE) {
            for (int blockX = 0; blockX < source.getWidth(); blockX += BLOCK_SIZE) {
                int[][][] reconstructed = new int[3][BLOCK_SIZE][BLOCK_SIZE];
                for (int channel = 0; channel < 3; channel++) {
                    double[][] samples = readBlock(source, blockX, blockY, channel);
                    double[][] coefficients = forward(samples);
                    for (int row = 0; row < BLOCK_SIZE; row++) {
                        for (int column = 0; column < BLOCK_SIZE; column++) {
                            int quantized = (int) Math.round(coefficients[row][column] / quantization[row][column]);
                            if (quantized != 0) {
                                nonZeroCoefficients++;
                            }
                            totalCoefficients++;
                            coefficients[row][column] = quantized * quantization[row][column];
                        }
                    }
                    double[][] restored = inverse(coefficients);
                    for (int row = 0; row < BLOCK_SIZE; row++) {
                        for (int column = 0; column < BLOCK_SIZE; column++) {
                            reconstructed[channel][row][column] = ImageTransforms.clamp(
                                (int) Math.round(restored[row][column] + 128.0)
                            );
                        }
                    }
                }

                writeBlock(output, blockX, blockY, reconstructed);
            }
        }

        double retainedPercent = totalCoefficients == 0
            ? 0.0
            : nonZeroCoefficients * 100.0 / totalCoefficients;
        double psnr = ImageTransforms.psnr(source, output);
        return new Result(output, safeQuality, retainedPercent, psnr);
    }

    private static double[][] readBlock(BufferedImage source, int blockX, int blockY, int channel) {
        double[][] values = new double[BLOCK_SIZE][BLOCK_SIZE];
        int shift = 16 - channel * 8;
        for (int row = 0; row < BLOCK_SIZE; row++) {
            int y = Math.min(source.getHeight() - 1, blockY + row);
            for (int column = 0; column < BLOCK_SIZE; column++) {
                int x = Math.min(source.getWidth() - 1, blockX + column);
                int value = (source.getRGB(x, y) >>> shift) & 0xff;
                values[row][column] = value - 128.0;
            }
        }
        return values;
    }

    private static void writeBlock(
        BufferedImage output,
        int blockX,
        int blockY,
        int[][][] channels
    ) {
        for (int row = 0; row < BLOCK_SIZE && blockY + row < output.getHeight(); row++) {
            for (int column = 0; column < BLOCK_SIZE && blockX + column < output.getWidth(); column++) {
                int rgb = (channels[0][row][column] << 16)
                    | (channels[1][row][column] << 8)
                    | channels[2][row][column];
                output.setRGB(blockX + column, blockY + row, rgb);
            }
        }
    }

    private static double[][] forward(double[][] samples) {
        double[][] coefficients = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int v = 0; v < BLOCK_SIZE; v++) {
                double sum = 0.0;
                for (int x = 0; x < BLOCK_SIZE; x++) {
                    for (int y = 0; y < BLOCK_SIZE; y++) {
                        sum += samples[x][y] * COSINE[x][u] * COSINE[y][v];
                    }
                }
                coefficients[u][v] = 0.25 * scale(u) * scale(v) * sum;
            }
        }
        return coefficients;
    }

    private static double[][] inverse(double[][] coefficients) {
        double[][] samples = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int x = 0; x < BLOCK_SIZE; x++) {
            for (int y = 0; y < BLOCK_SIZE; y++) {
                double sum = 0.0;
                for (int u = 0; u < BLOCK_SIZE; u++) {
                    for (int v = 0; v < BLOCK_SIZE; v++) {
                        sum += scale(u) * scale(v) * coefficients[u][v] * COSINE[x][u] * COSINE[y][v];
                    }
                }
                samples[x][y] = 0.25 * sum;
            }
        }
        return samples;
    }

    private static int[][] scaledQuantization(int quality) {
        int scale = quality < 50 ? 5000 / quality : 200 - quality * 2;
        int[][] table = new int[BLOCK_SIZE][BLOCK_SIZE];
        for (int row = 0; row < BLOCK_SIZE; row++) {
            for (int column = 0; column < BLOCK_SIZE; column++) {
                int value = (BASE_QUANTIZATION[row][column] * scale + 50) / 100;
                table[row][column] = Math.max(1, Math.min(255, value));
            }
        }
        return table;
    }

    private static double scale(int index) {
        return index == 0 ? 1.0 / Math.sqrt(2.0) : 1.0;
    }

    private static double[][] createCosineTable() {
        double[][] values = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int position = 0; position < BLOCK_SIZE; position++) {
            for (int frequency = 0; frequency < BLOCK_SIZE; frequency++) {
                values[position][frequency] = Math.cos(
                    (2.0 * position + 1.0) * frequency * Math.PI / 16.0
                );
            }
        }
        return values;
    }

    public record Result(
        BufferedImage image,
        int quality,
        double retainedCoefficientPercent,
        double psnr
    ) {
    }
}

