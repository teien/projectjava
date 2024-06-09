import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;


public class SettingUI extends JDialog {

    private final JCheckBox cpuCheckBox;
    private final JCheckBox ramCheckBox;
    private final JCheckBox ssdCheckBox;
    private final JCheckBox networkCheckBox;
    private final JCheckBox gpuCheckBox;
    private final JCheckBox weatherCheckBox;
    private final JCheckBox processCheckBox;


    private boolean settingsAccepted;


    public SettingUI(JFrame parent) {
        super(parent, "Settings", true);
        setSize(600, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 3));
        cpuCheckBox = new JCheckBox("Show CPU Info", true);
        ramCheckBox = new JCheckBox("Show RAM Info", true);
        ssdCheckBox = new JCheckBox("Show SSD Info", true);
        networkCheckBox = new JCheckBox("Show Network Info", true);
        gpuCheckBox = new JCheckBox("Show GPU Info", true);
        weatherCheckBox = new JCheckBox("Show Weather Info", true);
        processCheckBox = new JCheckBox("Show Process Info", true);

        checkBoxPanel.add(cpuCheckBox);
        checkBoxPanel.add(ramCheckBox);
        checkBoxPanel.add(ssdCheckBox);
        checkBoxPanel.add(networkCheckBox);
        checkBoxPanel.add(gpuCheckBox);
        checkBoxPanel.add(weatherCheckBox);
        checkBoxPanel.add(processCheckBox);
        add(checkBoxPanel, BorderLayout.CENTER);
        JPanel buttonsPanel = getjPanel();
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private @NotNull JPanel getjPanel() {
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            settingsAccepted = true;
            setVisible(false);
        });


        cancelButton.addActionListener(e -> setVisible(false));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(okButton);

        buttonsPanel.add(cancelButton);
        return buttonsPanel;
    }

    public boolean isSettingsAccepted() {
        return settingsAccepted;
    }

    public boolean isCpuSelected() {
        return cpuCheckBox.isSelected();
    }

    public boolean isRamSelected() {
        return ramCheckBox.isSelected();
    }

    public boolean isSsdSelected() {

        return ssdCheckBox.isSelected();
    }

    public boolean isNetworkSelected() {
        return networkCheckBox.isSelected();
    }

    public boolean isGpuSelected() {

        return gpuCheckBox.isSelected();
    }

    public boolean isWeatherSelected() {

        return weatherCheckBox.isSelected();
    }
    public boolean isProcessSelected() {

        return processCheckBox.isSelected();
    }

}
