package ca.ernestwong.multimedia.audio;

import ca.ernestwong.multimedia.demo.DemoMedia;
import ca.ernestwong.multimedia.ui.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class AudioExplorerPanel extends JPanel {
    private final WaveformPanel waveformPanel = new WaveformPanel();
    private final JComboBox<String> channelSelector = new JComboBox<String>();
    private final JLabel fileLabel = new JLabel("Generated stereo demo");
    private final JLabel formatLabel = new JLabel();
    private final JTextArea resultsArea = new JTextArea();
    private final JButton analyzeButton = Theme.button("Analyze Huffman coding");
    private WavData wavData;

    public AudioExplorerPanel() {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(createToolbar(), BorderLayout.NORTH);
        add(createMainArea(), BorderLayout.CENTER);
        loadGeneratedDemo();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 8));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.PANEL_SOFT),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton openButton = Theme.button("Open WAV");
        JButton demoButton = Theme.button("Load demo");
        buttons.add(openButton);
        buttons.add(demoButton);
        buttons.add(channelSelector);
        buttons.add(analyzeButton);
        toolbar.add(buttons, BorderLayout.WEST);

        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 2));
        fileLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        formatLabel.setForeground(Theme.MUTED);
        labels.add(fileLabel);
        labels.add(formatLabel);
        toolbar.add(labels, BorderLayout.CENTER);

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                chooseWaveFile();
            }
        });
        demoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                loadGeneratedDemo();
            }
        });
        channelSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                waveformPanel.setChannelMode(Math.max(0, channelSelector.getSelectedIndex()));
            }
        });
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                analyzeSelectedChannel();
            }
        });
        return toolbar;
    }

    private JPanel createMainArea() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        waveformPanel.setPreferredSize(new Dimension(900, 430));
        waveformPanel.setBorder(BorderFactory.createLineBorder(Theme.PANEL_SOFT));
        content.add(waveformPanel, BorderLayout.CENTER);

        resultsArea.setEditable(false);
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setRows(8);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resultsArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setPreferredSize(new Dimension(900, 165));
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.PANEL_SOFT));
        content.add(scrollPane, BorderLayout.SOUTH);
        return content;
    }

    private void chooseWaveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PCM WAV audio", "wav"));
        int choice = chooser.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            loadWaveFile(chooser.getSelectedFile().toPath(), chooser.getSelectedFile().getName());
        }
    }

    private void loadGeneratedDemo() {
        try {
            Path demoPath = Files.createTempFile("multimedia-lab-demo-", ".wav");
            demoPath.toFile().deleteOnExit();
            DemoMedia.writeDemoWave(demoPath);
            loadWaveFile(demoPath, "Generated stereo demo");
            if (wavData != null) {
                resultsArea.setText(formatReport(analyzeChannel(0)));
                resultsArea.setCaretPosition(0);
            }
        } catch (Exception exception) {
            showError("The demo audio could not be created", exception);
        }
    }

    private void loadWaveFile(Path path, String displayName) {
        try {
            WavData loaded = WavReader.read(path);
            wavData = loaded;
            waveformPanel.setWavData(loaded);
            updateChannelSelector(loaded.channels());
            fileLabel.setText(displayName);
            formatLabel.setText(String.format(
                "%s Hz   %s bit PCM   %s   %.2f seconds   %,d frames",
                loaded.sampleRate(),
                loaded.bitsPerSample(),
                loaded.channels() == 1 ? "Mono" : "Stereo",
                loaded.durationSeconds(),
                loaded.frameCount()
            ));
            resultsArea.setText(
                "Choose a channel and run the Huffman analysis. "
                    + "The program will encode the samples, decode them, and verify the result."
            );
        } catch (Exception exception) {
            showError("The WAV file could not be opened", exception);
        }
    }

    private void updateChannelSelector(int channels) {
        channelSelector.removeAllItems();
        if (channels == 2) {
            channelSelector.addItem("Both channels");
        }
        for (int index = 0; index < channels; index++) {
            channelSelector.addItem("Channel " + (index + 1));
        }
        channelSelector.setSelectedIndex(0);
        waveformPanel.setChannelMode(channels == 2 ? 0 : 1);
    }

    private void analyzeSelectedChannel() {
        if (wavData == null) {
            return;
        }
        int selected = channelSelector.getSelectedIndex();
        int channel = wavData.channels() == 2 ? Math.max(0, selected - 1) : selected;
        analyzeButton.setEnabled(false);
        resultsArea.setText("Building a Huffman tree and verifying the encoded samples...");

        SwingWorker<AnalysisReport, Void> worker = new SwingWorker<AnalysisReport, Void>() {
            @Override
            protected AnalysisReport doInBackground() {
                return analyzeChannel(channel);
            }

            @Override
            protected void done() {
                analyzeButton.setEnabled(true);
                try {
                    resultsArea.setText(formatReport(get()));
                    resultsArea.setCaretPosition(0);
                } catch (Exception exception) {
                    showError("The Huffman analysis could not be completed", exception);
                }
            }
        };
        worker.execute();
    }

    private AnalysisReport analyzeChannel(int channel) {
        int[] source = wavData.channel(channel);
        HuffmanCodec.Model model = HuffmanCodec.build(source, wavData.bitsPerSample());
        HuffmanCodec.EncodedData encoded = HuffmanCodec.encode(source, model);
        int[] decoded = HuffmanCodec.decode(encoded, model);
        boolean verified = Arrays.equals(source, decoded);
        return new AnalysisReport(channel, model, encoded, verified);
    }

    private String formatReport(AnalysisReport report) {
        HuffmanCodec.Statistics statistics = report.model().statistics();
        DecimalFormat decimal = new DecimalFormat("0.000");
        StringBuilder text = new StringBuilder();
        text.append("HUFFMAN ANALYSIS   Channel ").append(report.channel() + 1).append('\n');
        text.append('\n');
        text.append("Samples:            ").append(String.format("%,d", statistics.symbolCount())).append('\n');
        text.append("Distinct values:    ").append(String.format("%,d", statistics.distinctSymbols())).append('\n');
        text.append("Entropy:            ").append(decimal.format(statistics.entropyBitsPerSymbol())).append(" bits per sample\n");
        text.append("Average code:       ").append(decimal.format(statistics.averageCodeLength())).append(" bits per sample\n");
        text.append("Original data:      ").append(formatBits(statistics.originalBits())).append('\n');
        text.append("Encoded data:       ").append(formatBits(statistics.encodedBits())).append('\n');
        text.append("Compression ratio:  ").append(decimal.format(statistics.compressionRatio())).append(" to 1\n");
        text.append("Lossless check:     ").append(report.verified() ? "PASSED" : "FAILED").append('\n');
        text.append('\n');
        text.append("Most common sample values\n");
        List<HuffmanCodec.SymbolFrequency> common = HuffmanCodec.mostCommonSymbols(report.model(), 6);
        for (HuffmanCodec.SymbolFrequency value : common) {
            text.append(String.format(
                "  %7d   %,8d occurrences   code %s%n",
                value.symbol(),
                value.frequency(),
                value.code()
            ));
        }
        return text.toString();
    }

    private String formatBits(long bits) {
        double kibibytes = bits / 8.0 / 1024.0;
        return String.format("%,d bits   %.2f KiB", bits, kibibytes);
    }

    private void showError(String message, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(
            this,
            message + ".\n\n" + cause.getMessage(),
            "Multimedia Systems Lab",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private record AnalysisReport(
        int channel,
        HuffmanCodec.Model model,
        HuffmanCodec.EncodedData encoded,
        boolean verified
    ) {
    }
}
