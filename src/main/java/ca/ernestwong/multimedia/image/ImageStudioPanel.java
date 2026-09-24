package ca.ernestwong.multimedia.image;

import ca.ernestwong.multimedia.demo.DemoMedia;
import ca.ernestwong.multimedia.ui.ImageCanvas;
import ca.ernestwong.multimedia.ui.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class ImageStudioPanel extends JPanel {
    private static final String[] OPERATIONS = {
        "Grayscale",
        "YUV brightness",
        "YUV saturation",
        "Monochrome colourization",
        "Ordered dithering",
        "Floyd Steinberg dithering",
        "DCT compression",
        "Fourier spectrum"
    };

    private final ImageCanvas originalCanvas = new ImageCanvas();
    private final ImageCanvas resultCanvas = new ImageCanvas();
    private final JComboBox<String> operationSelector = new JComboBox<String>(OPERATIONS);
    private final JSlider parameterSlider = new JSlider(1, 100, 55);
    private final JLabel parameterLabel = new JLabel("Quality 55");
    private final JButton shadowButton = Theme.button("Shadow colour");
    private final JButton highlightButton = Theme.button("Highlight colour");
    private final JLabel fileLabel = new JLabel("Generated demonstration image");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JButton applyButton = Theme.button("Apply transform");
    private Color shadowColor = new Color(22, 42, 68);
    private Color highlightColor = new Color(242, 177, 92);
    private BufferedImage originalImage;
    private BufferedImage resultImage;

    public ImageStudioPanel() {
        super(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(createToolbar(), BorderLayout.NORTH);
        add(createCanvasArea(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        loadDemo();
        updateQualityControls();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 8));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.PANEL_SOFT),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 6));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton openButton = Theme.button("Open image");
        JButton demoButton = Theme.button("Load demo");
        JButton resetButton = Theme.button("Reset");
        JButton saveButton = Theme.button("Save PNG");
        actions.add(openButton);
        actions.add(demoButton);
        actions.add(operationSelector);
        actions.add(applyButton);
        actions.add(resetButton);
        actions.add(saveButton);
        controls.add(actions);

        JPanel parameters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        parameterSlider.setPreferredSize(new java.awt.Dimension(180, 32));
        parameters.add(parameterLabel);
        parameters.add(parameterSlider);
        parameters.add(shadowButton);
        parameters.add(highlightButton);
        controls.add(parameters);
        toolbar.add(controls, BorderLayout.WEST);

        fileLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        toolbar.add(fileLabel, BorderLayout.CENTER);

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                chooseImage();
            }
        });
        demoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                loadDemo();
            }
        });
        operationSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                updateQualityControls();
            }
        });
        parameterSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                updateParameterLabel();
            }
        });
        shadowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Color selected = JColorChooser.showDialog(
                    ImageStudioPanel.this,
                    "Choose a shadow colour",
                    shadowColor
                );
                if (selected != null) {
                    shadowColor = selected;
                    updateColourButtons();
                }
            }
        });
        highlightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Color selected = JColorChooser.showDialog(
                    ImageStudioPanel.this,
                    "Choose a highlight colour",
                    highlightColor
                );
                if (selected != null) {
                    highlightColor = selected;
                    updateColourButtons();
                }
            }
        });
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                applyTransform();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                resetResult();
            }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveResult();
            }
        });
        return toolbar;
    }

    private JPanel createCanvasArea() {
        JPanel canvases = new JPanel(new GridLayout(1, 2, 12, 0));
        canvases.add(wrapCanvas("ORIGINAL", originalCanvas));
        canvases.add(wrapCanvas("PROCESSED", resultCanvas));
        return canvases;
    }

    private JPanel wrapCanvas(String title, ImageCanvas canvas) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(title, SwingConstants.LEFT);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        label.setForeground(Theme.MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 2));
        panel.add(label, BorderLayout.NORTH);
        canvas.setBorder(BorderFactory.createLineBorder(Theme.PANEL_SOFT));
        panel.add(canvas, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.PANEL_SOFT),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        statusLabel.setForeground(Theme.MUTED);
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Image files",
            "png",
            "jpg",
            "jpeg",
            "bmp",
            "gif"
        ));
        int choice = chooser.showOpenDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            loadImageFile(chooser.getSelectedFile());
        }
    }

    private void loadImageFile(File file) {
        try {
            BufferedImage loaded = ImageIO.read(file);
            if (loaded == null) {
                throw new IllegalArgumentException("The selected file is not a supported image");
            }
            setOriginal(ImageTransforms.copy(loaded), file.getName());
        } catch (Exception exception) {
            showError("The image could not be opened", exception);
        }
    }

    private void loadDemo() {
        setOriginal(DemoMedia.createDemoImage(), "Generated demonstration image");
        operationSelector.setSelectedItem("Monochrome colourization");
        resultImage = ImageTransforms.colorizeMonochrome(
            originalImage,
            shadowColor,
            highlightColor,
            1.0
        );
        resultCanvas.setImage(resultImage);
        statusLabel.setText("Demo preview with a navy to amber monochrome colour map.");
    }

    private void setOriginal(BufferedImage image, String name) {
        originalImage = image;
        resultImage = ImageTransforms.copy(image);
        originalCanvas.setImage(originalImage);
        resultCanvas.setImage(resultImage);
        fileLabel.setText(name);
        statusLabel.setText(String.format(
            "%d by %d pixels. Choose a transform to compare the result.",
            image.getWidth(),
            image.getHeight()
        ));
    }

    private void resetResult() {
        if (originalImage != null) {
            resultImage = ImageTransforms.copy(originalImage);
            resultCanvas.setImage(resultImage);
            statusLabel.setText("The processed view has been reset to the original image.");
        }
    }

    private void updateQualityControls() {
        String operation = String.valueOf(operationSelector.getSelectedItem());
        boolean sliderEnabled = false;
        if ("DCT compression".equals(operation)) {
            configureSlider(1, 100, 55);
            sliderEnabled = true;
        } else if ("YUV brightness".equals(operation)) {
            configureSlider(-100, 100, 20);
            sliderEnabled = true;
        } else if ("YUV saturation".equals(operation)) {
            configureSlider(0, 200, 140);
            sliderEnabled = true;
        } else if ("Monochrome colourization".equals(operation)) {
            configureSlider(0, 100, 100);
            sliderEnabled = true;
        }
        parameterSlider.setEnabled(sliderEnabled);
        parameterLabel.setEnabled(sliderEnabled);
        boolean colourizeSelected = "Monochrome colourization".equals(operation);
        shadowButton.setVisible(colourizeSelected);
        highlightButton.setVisible(colourizeSelected);
        updateParameterLabel();
        updateColourButtons();
    }

    private void configureSlider(int minimum, int maximum, int value) {
        parameterSlider.setMinimum(minimum);
        parameterSlider.setMaximum(maximum);
        parameterSlider.setValue(value);
    }

    private void updateParameterLabel() {
        String operation = String.valueOf(operationSelector.getSelectedItem());
        int value = parameterSlider.getValue();
        if ("DCT compression".equals(operation)) {
            parameterLabel.setText("Quality: " + value);
        } else if ("YUV brightness".equals(operation)) {
            parameterLabel.setText("Luma: " + (value >= 0 ? "+" : "") + value);
        } else if ("YUV saturation".equals(operation)) {
            parameterLabel.setText("Saturation: " + value + "%");
        } else if ("Monochrome colourization".equals(operation)) {
            parameterLabel.setText("Colour blend: " + value + "%");
        } else {
            parameterLabel.setText("No parameter");
        }
    }

    private void updateColourButtons() {
        styleColourButton(shadowButton, shadowColor);
        styleColourButton(highlightButton, highlightColor);
    }

    private void styleColourButton(JButton button, Color colour) {
        button.setBackground(colour);
        button.setForeground(ImageTransforms.luminance(colour.getRGB()) < 135 ? Color.WHITE : Color.BLACK);
    }

    private void applyTransform() {
        if (originalImage == null) {
            return;
        }
        String operation = String.valueOf(operationSelector.getSelectedItem());
        int parameter = parameterSlider.getValue();
        applyButton.setEnabled(false);
        statusLabel.setText("Applying " + operation.toLowerCase() + "...");

        SwingWorker<OperationResult, Void> worker = new SwingWorker<OperationResult, Void>() {
            @Override
            protected OperationResult doInBackground() {
                if ("Grayscale".equals(operation)) {
                    return new OperationResult(ImageTransforms.grayscale(originalImage), "Converted to perceptual grayscale.");
                }
                if ("YUV brightness".equals(operation)) {
                    return new OperationResult(
                        ImageTransforms.adjustYuvBrightness(originalImage, parameter),
                        "Adjusted Y luma by " + (parameter >= 0 ? "+" : "") + parameter
                            + " while retaining the original chroma values."
                    );
                }
                if ("YUV saturation".equals(operation)) {
                    return new OperationResult(
                        ImageTransforms.adjustYuvSaturation(originalImage, parameter / 100.0),
                        "Scaled the U and V chroma channels to " + parameter + "% while retaining Y luma."
                    );
                }
                if ("Monochrome colourization".equals(operation)) {
                    return new OperationResult(
                        ImageTransforms.colorizeMonochrome(
                            originalImage,
                            shadowColor,
                            highlightColor,
                            parameter / 100.0
                        ),
                        "Mapped image luminance between the selected shadow and highlight colours at "
                            + parameter + "% strength."
                    );
                }
                if ("Ordered dithering".equals(operation)) {
                    return new OperationResult(
                        ImageTransforms.orderedDither(originalImage),
                        "Applied a repeating four by four Bayer threshold matrix."
                    );
                }
                if ("Floyd Steinberg dithering".equals(operation)) {
                    return new OperationResult(
                        ImageTransforms.floydSteinbergDither(originalImage),
                        "Applied binary quantization with Floyd Steinberg error diffusion."
                    );
                }
                if ("DCT compression".equals(operation)) {
                    DctCodec.Result result = DctCodec.compress(originalImage, parameter);
                    DecimalFormat decimal = new DecimalFormat("0.00");
                    String message = "DCT quality " + result.quality()
                        + ". PSNR " + decimal.format(result.psnr()) + " dB. "
                        + decimal.format(result.retainedCoefficientPercent())
                        + "% of quantized coefficients remain nonzero.";
                    return new OperationResult(result.image(), message);
                }
                FourierTransform.Result result = FourierTransform.spectrum(originalImage);
                String message = "Rendered a logarithmic Fourier magnitude spectrum at "
                    + result.width() + " by " + result.height() + " pixels.";
                return new OperationResult(result.image(), message);
            }

            @Override
            protected void done() {
                applyButton.setEnabled(true);
                try {
                    OperationResult result = get();
                    resultImage = result.image();
                    resultCanvas.setImage(resultImage);
                    statusLabel.setText(result.message());
                } catch (Exception exception) {
                    showError("The image transform could not be completed", exception);
                }
            }
        };
        worker.execute();
    }

    private void saveResult() {
        if (resultImage == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("processed-image.png"));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destination = chooser.getSelectedFile();
        if (!destination.getName().toLowerCase().endsWith(".png")) {
            destination = new File(destination.getParentFile(), destination.getName() + ".png");
        }
        try {
            ImageIO.write(resultImage, "png", destination);
            statusLabel.setText("Saved " + destination.getAbsolutePath());
        } catch (Exception exception) {
            showError("The processed image could not be saved", exception);
        }
    }

    private void showError(String message, Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        JOptionPane.showMessageDialog(
            this,
            message + ".\n\n" + cause.getMessage(),
            "Multimedia Systems Lab",
            JOptionPane.ERROR_MESSAGE
        );
        applyButton.setEnabled(true);
    }

    private record OperationResult(BufferedImage image, String message) {
    }
}
