package ca.ernestwong.multimedia;

import ca.ernestwong.multimedia.audio.AudioExplorerPanel;
import ca.ernestwong.multimedia.image.ImageStudioPanel;
import ca.ernestwong.multimedia.ui.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public final class MultimediaLabApp {
    private MultimediaLabApp() {
    }

    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createWindow();
            }
        });
    }

    private static void createWindow() {
        Theme.install();

        JFrame frame = new JFrame("Multimedia Systems Lab");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1050, 720));
        frame.setSize(1320, 850);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));

        JLabel title = new JLabel("MULTIMEDIA SYSTEMS LAB", SwingConstants.LEFT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(4, 2, 14, 2));
        root.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        tabs.addTab("Audio and Huffman", new AudioExplorerPanel());
        tabs.addTab("Image Transforms", new ImageStudioPanel());
        root.add(tabs, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setVisible(true);
    }
}
