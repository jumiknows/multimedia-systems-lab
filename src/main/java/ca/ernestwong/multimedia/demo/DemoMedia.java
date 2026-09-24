package ca.ernestwong.multimedia.demo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DemoMedia {
    private static final int SAMPLE_RATE = 44_100;
    private static final int DEMO_SECONDS = 2;

    private DemoMedia() {
    }

    public static Path writeDemoWave(Path path) throws IOException {
        int frames = SAMPLE_RATE * DEMO_SECONDS;
        int channels = 2;
        int bitsPerSample = 16;
        int bytesPerSample = bitsPerSample / 8;
        int dataSize = frames * channels * bytesPerSample;

        try (DataOutputStream output = new DataOutputStream(
            new BufferedOutputStream(Files.newOutputStream(path)))) {
            writeAscii(output, "RIFF");
            writeLittleEndianInt(output, 36 + dataSize);
            writeAscii(output, "WAVE");
            writeAscii(output, "fmt ");
            writeLittleEndianInt(output, 16);
            writeLittleEndianShort(output, 1);
            writeLittleEndianShort(output, channels);
            writeLittleEndianInt(output, SAMPLE_RATE);
            writeLittleEndianInt(output, SAMPLE_RATE * channels * bytesPerSample);
            writeLittleEndianShort(output, channels * bytesPerSample);
            writeLittleEndianShort(output, bitsPerSample);
            writeAscii(output, "data");
            writeLittleEndianInt(output, dataSize);

            for (int index = 0; index < frames; index++) {
                double time = index / (double) SAMPLE_RATE;
                double fade = Math.min(1.0, Math.min(time * 4.0, (DEMO_SECONDS - time) * 4.0));
                short left = sample((Math.sin(2.0 * Math.PI * 220.0 * time)
                    + 0.35 * Math.sin(2.0 * Math.PI * 440.0 * time)) * fade);
                short right = sample((Math.sin(2.0 * Math.PI * 330.0 * time)
                    + 0.25 * Math.sin(2.0 * Math.PI * 660.0 * time)) * fade);
                writeLittleEndianShort(output, left);
                writeLittleEndianShort(output, right);
            }
        }
        return path;
    }

    public static BufferedImage createDemoImage() {
        int width = 800;
        int height = 520;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setPaint(new GradientPaint(0, 0, new Color(14, 37, 66), width, height, new Color(139, 62, 116)));
        graphics.fillRect(0, 0, width, height);

        graphics.setColor(new Color(34, 211, 238, 145));
        graphics.fillOval(70, 65, 250, 250);
        graphics.setColor(new Color(244, 114, 182, 135));
        graphics.fillOval(500, 210, 220, 220);

        graphics.setStroke(new BasicStroke(5.0f));
        graphics.setColor(new Color(255, 255, 255, 175));
        for (int index = 0; index < 9; index++) {
            int y = 70 + index * 46;
            graphics.drawLine(355, y, 355 + (index % 3) * 65 + 95, y);
        }

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        graphics.setColor(Color.WHITE);
        graphics.drawString("MULTIMEDIA", 52, 455);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        graphics.setColor(new Color(226, 232, 240));
        graphics.drawString("Pixels, frequencies, and perception", 55, 490);
        graphics.dispose();
        return image;
    }

    private static short sample(double value) {
        double clipped = Math.max(-1.0, Math.min(1.0, value * 0.55));
        return (short) Math.round(clipped * Short.MAX_VALUE);
    }

    private static void writeAscii(DataOutputStream output, String text) throws IOException {
        output.writeBytes(text);
    }

    private static void writeLittleEndianShort(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xff);
        output.writeByte((value >>> 8) & 0xff);
    }

    private static void writeLittleEndianInt(DataOutputStream output, int value) throws IOException {
        output.writeByte(value & 0xff);
        output.writeByte((value >>> 8) & 0xff);
        output.writeByte((value >>> 16) & 0xff);
        output.writeByte((value >>> 24) & 0xff);
    }
}

