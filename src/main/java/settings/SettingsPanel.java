package settings;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    private static JSpinner fontSizeSpinner1 = new JSpinner();
    private static JSpinner fontSizeSpinner2 = new JSpinner();
    private static JButton fontColorButton1 = new JButton();
    private static JButton fontColorButton2 = new JButton();
    private static final JButton bgColorButton = new JButton();
    private static JComboBox<String> fontTypeComboBox1 = new JComboBox<>();
    private static JComboBox<String> fontTypeComboBox2 = new JComboBox<>();
    private static Color selectedColor1 = Color.WHITE;
    private static Color selectedColor2 = Color.WHITE;
    private static Double selectedOpacity = 1.0;
    private static Color selectedBgColor = Color.BLACK;
    private static final JSlider opacitySlider = new JSlider(0, 100, 100);

    static {
        opacitySlider.setMajorTickSpacing(10);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        opacitySlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            selectedOpacity = source.getValue() / 100.0;
        });
    }

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Font Size
        gbc.gridx = 0;
        gbc.gridy = 0;
        contentPanel.add(new JLabel("Font Size 1:"), gbc);

        gbc.gridx = 1;
        fontSizeSpinner1 = new JSpinner(new SpinnerNumberModel(14, 4, 50, 1));
        contentPanel.add(fontSizeSpinner1, gbc);

        gbc.gridx = 2;
        contentPanel.add(new JLabel("Font Size 2:"), gbc);

        gbc.gridx = 3;
        fontSizeSpinner2 = new JSpinner(new SpinnerNumberModel(14, 8, 50, 1));
        contentPanel.add(fontSizeSpinner2, gbc);

        // Font Color
        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPanel.add(new JLabel("Font Color 1:"), gbc);

        gbc.gridx = 1;
        fontColorButton1 = new JButton("Select Color");
        fontColorButton1.setBackground(Color.BLACK);
        fontColorButton1.setForeground(Color.WHITE);
        fontColorButton1.addActionListener(e -> {
            selectedColor1 = JColorChooser.showDialog(null, "Choose Font Color 1", selectedColor1);
            if (selectedColor1 != null) {
                fontColorButton1.setBackground(selectedColor1);
            }
        });
        contentPanel.add(fontColorButton1, gbc);

        gbc.gridx = 2;
        contentPanel.add(new JLabel("Font Color 2:"), gbc);

        gbc.gridx = 3;
        fontColorButton2 = new JButton("Select Color");
        fontColorButton2.setBackground(Color.BLACK);
        fontColorButton2.setForeground(Color.WHITE);
        fontColorButton2.addActionListener(e -> {
            selectedColor2 = JColorChooser.showDialog(null, "Choose Font Color 2", selectedColor2);
            if (selectedColor2 != null) {
                fontColorButton2.setBackground(selectedColor2);
            }
        });
        contentPanel.add(fontColorButton2, gbc);

        // Font Type
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentPanel.add(new JLabel("Font Type 1:"), gbc);

        gbc.gridx = 1;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        fontTypeComboBox1 = new JComboBox<>(fontNames);
        fontTypeComboBox1.setSelectedItem("Arial");
        contentPanel.add(fontTypeComboBox1, gbc);

        gbc.gridx = 2;
        contentPanel.add(new JLabel("Font Type 2:"), gbc);

        gbc.gridx = 3;
        fontTypeComboBox2 = new JComboBox<>(fontNames);
        fontTypeComboBox2.setSelectedItem("Arial");
        contentPanel.add(fontTypeComboBox2, gbc);

        // Background Opacity
        gbc.gridx = 0;
        gbc.gridy = 3;
        contentPanel.add(new JLabel("Background Opacity:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;

        contentPanel.add(opacitySlider, gbc);
        gbc.gridwidth = 1;

        // Background Color
        gbc.gridx = 0;
        gbc.gridy = 4;
        contentPanel.add(new JLabel("Background Color:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        JButton bgColorButton = new JButton("Select Color");
        bgColorButton.setBackground(Color.BLACK);
        bgColorButton.setForeground(Color.WHITE);
        bgColorButton.addActionListener(e -> {
            selectedBgColor = JColorChooser.showDialog(null, "Choose Background Color", selectedBgColor);
            if (selectedBgColor != null) {
                bgColorButton.setBackground(selectedBgColor);
            }
        });
        contentPanel.add(bgColorButton, gbc);
        add(contentPanel, BorderLayout.CENTER);
    }

    public static int getSelectedFontSize1() {
        return (Integer) fontSizeSpinner1.getValue();
    }

    public static int getSelectedFontSize2() {
        return (Integer) fontSizeSpinner2.getValue();
    }

    public static Color getSelectedFontColor1() {
        return selectedColor1;
    }

    public static Color getSelectedFontColor2() {
        return selectedColor2;
    }

    public static String getSelectedFontType1() {
        return (String) fontTypeComboBox1.getSelectedItem();
    }

    public static String getSelectedFontType2() {
        return (String) fontTypeComboBox2.getSelectedItem();
    }

    public static Color getSelectedBgColor() {
        return selectedBgColor;
    }

    public static Double getOpacity() {
        return selectedOpacity;
    }
    public static Boolean checkSettings = false;
    public static void applySettings() {
        try {
            int fontSize1 = getSelectedFontSize1();
            int fontSize2 = getSelectedFontSize2();
            Color fontColor1 = getSelectedFontColor1();
            Color fontColor2 = getSelectedFontColor2();
            String fontType1 = getSelectedFontType1();
            String fontType2 = getSelectedFontType2();
            Double opacity = getOpacity();
            Color bgColor = getSelectedBgColor();
            SettingsLogger.saveSettings(fontType1, fontSize1, fontColor1, fontType2, fontSize2, fontColor2, opacity, bgColor);
            checkSettings = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadSettings() {
        try {
            JSONObject settings = SettingsLogger.loadSettings();
            fontSizeSpinner1.setValue(settings.getJSONObject("Style").getInt("fontSize1"));
            fontSizeSpinner2.setValue(settings.getJSONObject("Style").getInt("fontSize2"));
            fontTypeComboBox1.setSelectedItem(settings.getJSONObject("Style").getString("fontType1"));
            fontTypeComboBox2.setSelectedItem(settings.getJSONObject("Style").getString("fontType2"));
            fontColorButton1.setBackground(new Color(settings.getJSONObject("Style").getInt("fontColor1")));
            selectedColor1 = new Color(settings.getJSONObject("Style").getInt("fontColor1"));
            fontColorButton2.setBackground(new Color(settings.getJSONObject("Style").getInt("fontColor2")));
            selectedColor2 = new Color(settings.getJSONObject("Style").getInt("fontColor2"));
            selectedOpacity = settings.getJSONObject("Style").getDouble("opacity");
            opacitySlider.setValue((int) (selectedOpacity * 100));
            bgColorButton.setBackground(new Color(settings.getJSONObject("Style").getInt("bgColor"), true));
            selectedBgColor = new Color(settings.getJSONObject("Style").getInt("bgColor"), true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}