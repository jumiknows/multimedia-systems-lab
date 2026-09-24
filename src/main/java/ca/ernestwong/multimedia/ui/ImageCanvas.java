package ca.ernestwong.multimedia.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public final class ImageCanvas extends JPanel {
    private BufferedImage image;

    public ImageCanvas() {
        setBackground(new Color(10, 15, 25));
        setPreferredSize(new Dimension(520, 390));
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (image == null) {
            canvas.setColor(Theme.MUTED);
            canvas.drawString("No image loaded", 24, 32);
            canvas.dispose();
            return;
        }

        double widthScale = (getWidth() - 24.0) / image.getWidth();
        double heightScale = (getHeight() - 24.0) / image.getHeight();
        double scale = Math.min(widthScale, heightScale);
        int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int x = (getWidth() - drawWidth) / 2;
        int y = (getHeight() - drawHeight) / 2;

        canvas.drawImage(image, x, y, drawWidth, drawHeight, null);
        canvas.setColor(new Color(255, 255, 255, 45));
        canvas.drawRect(x, y, drawWidth - 1, drawHeight - 1);
        canvas.dispose();
    }
}

