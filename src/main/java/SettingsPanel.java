import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class SettingsPanel extends JPanel {

    private static JSpinner fontSizeSpinner = new JSpinner();
    private final JButton fontColorButton;
    private static JComboBox<String> fontTypeComboBox = new JComboBox<>();
    private static Color selectedColor;
    private static Double selectedOpacity;
    private static Color selectedBgColor = Color.BLACK;

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Tạo khoảng cách với viền của panel

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel fontSizeLabel = new JLabel("Font Size:");
        contentPanel.add(fontSizeLabel, gbc);

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(14, 10, 50, 1));
        gbc.gridx = 1;
        contentPanel.add(fontSizeSpinner, gbc);

        JLabel fontColorLabel = new JLabel("Font Color:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPanel.add(fontColorLabel, gbc);

        fontColorButton = new JButton("Select Color");
        fontColorButton.setBackground(Color.BLACK);
        fontColorButton.setForeground(Color.WHITE);
        fontColorButton.addActionListener(e -> {
            selectedColor = JColorChooser.showDialog(null, "Choose Font Color", Color.BLACK);
            if (selectedColor != null) {
                fontColorButton.setBackground(selectedColor);
            }
        });
        gbc.gridx = 1;
        contentPanel.add(fontColorButton, gbc);

        JLabel fontTypeLabel = new JLabel("Font Type:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentPanel.add(fontTypeLabel, gbc);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        fontTypeComboBox = new JComboBox<>(fontNames);
        gbc.gridx = 1;
        contentPanel.add(fontTypeComboBox, gbc);

        JLabel opacityLabel = new JLabel("Background Opacity:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        contentPanel.add(opacityLabel, gbc);

        JSlider opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setMajorTickSpacing(10);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        opacitySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                selectedOpacity = source.getValue() / 100.0;
            }
        });
        gbc.gridx = 1;
        contentPanel.add(opacitySlider, gbc);

        JLabel bgColorLabel = new JLabel("Background Color:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        contentPanel.add(bgColorLabel, gbc);

        JButton bgColorButton = new JButton("Select Color");
        bgColorButton.setBackground(Color.BLACK);
        bgColorButton.setForeground(Color.WHITE);
        bgColorButton.addActionListener(e -> {
            selectedBgColor = JColorChooser.showDialog(null, "Choose Background Color", Color.BLACK);
            if (selectedBgColor != null) {
                bgColorButton.setBackground(selectedBgColor);
            }
        });
        gbc.gridx = 1;
        contentPanel.add(bgColorButton, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    public static int getSelectedFontSize() {
        return (Integer) fontSizeSpinner.getValue();
    }

    public static Double getOpacity() {
        return selectedOpacity;
    }

    public static Color getSelectedFontColor() {
        if (selectedColor == null) {
            return Color.WHITE;
        }
        return selectedColor;
    }

    public static String getSelectedFontType() {
        return (String) fontTypeComboBox.getSelectedItem();
    }

    public static Color getSelectedBgColor() {
        return selectedBgColor;
    }
    static Boolean checkSettings = Boolean.FALSE;
    public static void applySettings() {
        try {
            int fontSize = getSelectedFontSize();
            Color fontColor = getSelectedFontColor();
            String fontType = getSelectedFontType();
            Double opacity = getOpacity();
            Color bgColor = getSelectedBgColor();
            SettingsLogger.saveSettings(fontType, fontSize, fontColor, opacity, bgColor);
            checkSettings = Boolean.TRUE;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
