package ca.ernestwong.multimedia;

import ca.ernestwong.multimedia.audio.HuffmanCodec;
import ca.ernestwong.multimedia.audio.WavData;
import ca.ernestwong.multimedia.audio.WavReader;
import ca.ernestwong.multimedia.demo.DemoMedia;
import ca.ernestwong.multimedia.image.DctCodec;
import ca.ernestwong.multimedia.image.FourierTransform;
import ca.ernestwong.multimedia.image.ImageTransforms;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] arguments) throws Exception {
        Map<String, CheckedTest> tests = new LinkedHashMap<String, CheckedTest>();
        tests.put("WAV parser reads generated stereo audio", new CheckedTest() {
            @Override
            public void run() throws Exception {
                testWavReader();
            }
        });
        tests.put("Huffman coding restores every symbol", new CheckedTest() {
            @Override
            public void run() {
                testHuffmanRoundTrip();
            }
        });
        tests.put("Ordered dithering produces only black and white", new CheckedTest() {
            @Override
            public void run() {
                testOrderedDither();
            }
        });
        tests.put("YUV brightness changes luma while retaining chroma", new CheckedTest() {
            @Override
            public void run() {
                testYuvBrightness();
            }
        });
        tests.put("YUV saturation can remove chroma", new CheckedTest() {
            @Override
            public void run() {
                testYuvSaturation();
            }
        });
        tests.put("Monochrome colourization respects both colour endpoints", new CheckedTest() {
            @Override
            public void run() {
                testMonochromeColourization();
            }
        });
        tests.put("DCT compression preserves image dimensions", new CheckedTest() {
            @Override
            public void run() {
                testDctCompression();
            }
        });
        tests.put("Fourier spectrum uses power of two dimensions", new CheckedTest() {
            @Override
            public void run() {
                testFourierSpectrum();
            }
        });

        int passed = 0;
        for (Map.Entry<String, CheckedTest> entry : tests.entrySet()) {
            try {
                entry.getValue().run();
                passed++;
                System.out.println("PASS  " + entry.getKey());
            } catch (Throwable failure) {
                System.err.println("FAIL  " + entry.getKey());
                failure.printStackTrace(System.err);
            }
        }

        System.out.println();
        System.out.println(passed + " of " + tests.size() + " tests passed");
        if (passed != tests.size()) {
            System.exit(1);
        }
    }

    private static void testWavReader() throws Exception {
        Path wave = Files.createTempFile("multimedia-lab-test-", ".wav");
        try {
            DemoMedia.writeDemoWave(wave);
            WavData data = WavReader.read(wave);
            require(data.channels() == 2, "Expected stereo audio");
            require(data.sampleRate() == 44_100, "Expected a 44.1 kHz sample rate");
            require(data.bitsPerSample() == 16, "Expected 16 bit PCM");
            require(data.frameCount() == 88_200, "Expected two seconds of audio");
            require(data.channel(0)[1_000] != data.channel(1)[1_000], "Channels should contain different tones");
        } finally {
            Files.deleteIfExists(wave);
        }
    }

    private static void testHuffmanRoundTrip() {
        int[] symbols = new int[4_000];
        for (int index = 0; index < symbols.length; index++) {
            if (index % 20 == 0) {
                symbols[index] = 3;
            } else if (index % 5 == 0) {
                symbols[index] = 2;
            } else {
                symbols[index] = 1;
            }
        }
        HuffmanCodec.Model model = HuffmanCodec.build(symbols, 16);
        HuffmanCodec.EncodedData encoded = HuffmanCodec.encode(symbols, model);
        int[] decoded = HuffmanCodec.decode(encoded, model);
        require(Arrays.equals(symbols, decoded), "Decoded symbols did not match the source");
        require(
            model.statistics().encodedBits() < model.statistics().originalBits(),
            "Skewed symbols should use fewer bits"
        );
    }

    private static void testOrderedDither() {
        BufferedImage source = DemoMedia.createDemoImage();
        BufferedImage output = ImageTransforms.orderedDither(source);
        for (int y = 0; y < output.getHeight(); y += 11) {
            for (int x = 0; x < output.getWidth(); x += 11) {
                int rgb = output.getRGB(x, y) & 0x00ffffff;
                require(rgb == 0x000000 || rgb == 0xffffff, "Dither output contained a gray pixel");
            }
        }
    }

    private static void testDctCompression() {
        BufferedImage source = createSmallImage(48, 40);
        DctCodec.Result result = DctCodec.compress(source, 65);
        require(result.image().getWidth() == source.getWidth(), "DCT changed image width");
        require(result.image().getHeight() == source.getHeight(), "DCT changed image height");
        require(Double.isFinite(result.psnr()) && result.psnr() > 10.0, "DCT PSNR was unexpectedly low");
        require(
            result.retainedCoefficientPercent() > 0.0 && result.retainedCoefficientPercent() <= 100.0,
            "Coefficient percentage was out of range"
        );
    }

    private static void testYuvBrightness() {
        BufferedImage source = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, new Color(70, 110, 160).getRGB());
        BufferedImage output = ImageTransforms.adjustYuvBrightness(source, 30);
        double[] before = yuv(source.getRGB(0, 0));
        double[] after = yuv(output.getRGB(0, 0));
        require(Math.abs((after[0] - before[0]) - 30.0) < 2.0, "Luma did not change by the requested amount");
        require(Math.abs(after[1] - before[1]) < 2.0, "U chroma changed unexpectedly");
        require(Math.abs(after[2] - before[2]) < 2.0, "V chroma changed unexpectedly");
    }

    private static void testYuvSaturation() {
        BufferedImage source = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, new Color(220, 70, 35).getRGB());
        int rgb = ImageTransforms.adjustYuvSaturation(source, 0.0).getRGB(0, 0);
        int red = (rgb >>> 16) & 0xff;
        int green = (rgb >>> 8) & 0xff;
        int blue = rgb & 0xff;
        require(Math.abs(red - green) <= 1 && Math.abs(green - blue) <= 1, "Zero saturation was not grayscale");
    }

    private static void testMonochromeColourization() {
        BufferedImage source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, Color.BLACK.getRGB());
        source.setRGB(1, 0, Color.WHITE.getRGB());
        Color shadow = new Color(15, 35, 60);
        Color highlight = new Color(240, 180, 95);
        BufferedImage output = ImageTransforms.colorizeMonochrome(source, shadow, highlight, 1.0);
        require((output.getRGB(0, 0) & 0x00ffffff) == (shadow.getRGB() & 0x00ffffff), "Black did not map to the shadow colour");
        require((output.getRGB(1, 0) & 0x00ffffff) == (highlight.getRGB() & 0x00ffffff), "White did not map to the highlight colour");
    }

    private static void testFourierSpectrum() {
        BufferedImage source = createSmallImage(96, 70);
        FourierTransform.Result result = FourierTransform.spectrum(source);
        require(Integer.bitCount(result.width()) == 1, "Spectrum width was not a power of two");
        require(Integer.bitCount(result.height()) == 1, "Spectrum height was not a power of two");
        require(result.image().getWidth() == result.width(), "Spectrum width did not match metadata");
        require(result.image().getHeight() == result.height(), "Spectrum height did not match metadata");
    }

    private static BufferedImage createSmallImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = x * 255 / Math.max(1, width - 1);
                int green = y * 255 / Math.max(1, height - 1);
                int blue = (x + y) * 255 / Math.max(1, width + height - 2);
                image.setRGB(x, y, (red << 16) | (green << 8) | blue);
            }
        }
        return image;
    }

    private static double[] yuv(int rgb) {
        double red = (rgb >>> 16) & 0xff;
        double green = (rgb >>> 8) & 0xff;
        double blue = rgb & 0xff;
        return new double[] {
            0.299 * red + 0.587 * green + 0.114 * blue,
            -0.168736 * red - 0.331264 * green + 0.5 * blue,
            0.5 * red - 0.418688 * green - 0.081312 * blue
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private interface CheckedTest {
        void run() throws Exception;
    }
}
