package ca.ernestwong.multimedia.demo;

import ca.ernestwong.multimedia.audio.AudioExplorerPanel;
import ca.ernestwong.multimedia.image.ImageStudioPanel;
import ca.ernestwong.multimedia.ui.Theme;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class ScreenshotGenerator {
    private ScreenshotGenerator() {
    }

    public static void main(String[] arguments) throws Exception {
        Path outputDirectory = arguments.length == 0
            ? Path.of("docs", "screenshots")
            : Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);

        final Exception[] failure = new Exception[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Theme.install();
                    render(new AudioExplorerPanel(), outputDirectory.resolve("audio-explorer.png"), 1240, 760);
                    render(new ImageStudioPanel(), outputDirectory.resolve("image-studio.png"), 1320, 760);
                } catch (Exception exception) {
                    failure[0] = exception;
                }
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        System.out.println("Screenshots written to " + outputDirectory.toAbsolutePath());
    }

    private static void render(JPanel panel, Path destination, int width, int height) throws Exception {
        panel.setSize(width, height);
        layout(panel);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", destination.toFile());
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layout((Container) child);
            }
        }
    }
}
