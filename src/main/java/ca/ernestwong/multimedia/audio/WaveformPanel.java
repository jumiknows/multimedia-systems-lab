package ca.ernestwong.multimedia.audio;

import ca.ernestwong.multimedia.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public final class WaveformPanel extends JPanel {
    private WavData wavData;
    private int channelMode;

    public WaveformPanel() {
        setBackground(new Color(10, 15, 25));
    }

    public void setWavData(WavData wavData) {
        this.wavData = wavData;
        repaint();
    }

    public void setChannelMode(int channelMode) {
        this.channelMode = channelMode;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(canvas);
        if (wavData == null || wavData.frameCount() == 0) {
            canvas.setColor(Theme.MUTED);
            canvas.drawString("Open a PCM WAV file to inspect its channels", 24, 32);
            canvas.dispose();
            return;
        }

        int top = 20;
        int bottom = getHeight() - 20;
        if (channelMode == 0 && wavData.channels() == 2) {
            int middle = getHeight() / 2;
            drawChannel(canvas, wavData.channel(0), top, middle - 10, Theme.CYAN, "LEFT");
            drawChannel(canvas, wavData.channel(1), middle + 10, bottom, Theme.VIOLET, "RIGHT");
        } else {
            int channel = Math.max(0, Math.min(wavData.channels() - 1, channelMode - 1));
            Color color = channel == 0 ? Theme.CYAN : Theme.VIOLET;
            drawChannel(canvas, wavData.channel(channel), top, bottom, color, "CHANNEL " + (channel + 1));
        }
        canvas.dispose();
    }

    private void drawGrid(Graphics2D canvas) {
        canvas.setColor(new Color(148, 163, 184, 26));
        canvas.setStroke(new BasicStroke(1.0f));
        for (int index = 1; index < 8; index++) {
            int x = index * getWidth() / 8;
            canvas.drawLine(x, 0, x, getHeight());
        }
        for (int index = 1; index < 6; index++) {
            int y = index * getHeight() / 6;
            canvas.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawChannel(
        Graphics2D canvas,
        int[] samples,
        int top,
        int bottom,
        Color color,
        String label
    ) {
        int width = Math.max(1, getWidth());
        int centre = (top + bottom) / 2;
        int amplitude = Math.max(1, (bottom - top) / 2 - 6);
        int maximum = wavData.bitsPerSample() == 8 ? 128 : 32768;

        canvas.setColor(new Color(148, 163, 184, 70));
        canvas.drawLine(0, centre, width, centre);
        canvas.setColor(color);
        canvas.setStroke(new BasicStroke(1.1f));

        for (int x = 0; x < width; x++) {
            int start = x * samples.length / width;
            int end = Math.max(start + 1, (x + 1) * samples.length / width);
            end = Math.min(end, samples.length);
            int minimum = 0;
            int maximumSample = 0;
            for (int index = start; index < end; index++) {
                minimum = Math.min(minimum, samples[index]);
                maximumSample = Math.max(maximumSample, samples[index]);
            }
            int yTop = centre - (int) Math.round(maximumSample / (double) maximum * amplitude);
            int yBottom = centre - (int) Math.round(minimum / (double) maximum * amplitude);
            canvas.drawLine(x, yTop, x, yBottom);
        }

        canvas.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 190));
        canvas.drawString(label, 12, top + 16);
    }
}

