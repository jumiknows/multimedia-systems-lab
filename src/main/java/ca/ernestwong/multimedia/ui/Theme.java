package ca.ernestwong.multimedia.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;

public final class Theme {
    public static final Color INK = new Color(17, 24, 39);
    public static final Color PANEL = new Color(27, 38, 55);
    public static final Color PANEL_SOFT = new Color(37, 51, 70);
    public static final Color TEXT = new Color(241, 245, 249);
    public static final Color MUTED = new Color(148, 163, 184);
    public static final Color CYAN = new Color(34, 211, 238);
    public static final Color VIOLET = new Color(167, 139, 250);
    public static final Color GREEN = new Color(52, 211, 153);
    public static final Color PINK = new Color(244, 114, 182);

    private Theme() {
    }

    public static void install() {
        UIManager.put("Panel.background", INK);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", PANEL_SOFT);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("ComboBox.background", PANEL_SOFT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("TabbedPane.background", INK);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", PANEL);
        UIManager.put("Slider.background", INK);
        UIManager.put("Slider.foreground", TEXT);
        UIManager.put("TextArea.background", PANEL);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("ScrollPane.background", PANEL);
    }

    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(71, 85, 105)),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        return button;
    }
}

