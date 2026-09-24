package ca.ernestwong.multimedia.image;

import ca.ernestwong.multimedia.ui.Theme;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class ImageStudioApp {
    private ImageStudioApp() {
    }

    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Theme.install();
                JFrame frame = new JFrame("Image Transform Studio");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(new ImageStudioPanel());
                frame.setMinimumSize(new Dimension(1000, 650));
                frame.setSize(1320, 780);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
