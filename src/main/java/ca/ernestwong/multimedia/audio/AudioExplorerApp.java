package ca.ernestwong.multimedia.audio;

import ca.ernestwong.multimedia.ui.Theme;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class AudioExplorerApp {
    private AudioExplorerApp() {
    }

    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Theme.install();
                JFrame frame = new JFrame("Audio and Huffman Explorer");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(new AudioExplorerPanel());
                frame.setMinimumSize(new Dimension(950, 680));
                frame.setSize(1180, 790);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
