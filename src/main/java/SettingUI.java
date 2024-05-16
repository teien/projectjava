import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingUI extends JDialog {

    private JCheckBox cpuCheckBox;
    private JCheckBox ramCheckBox;
    private JCheckBox ssdCheckBox;
    private JCheckBox networkCheckBox;
    private boolean settingsAccepted;

    public SettingUI(JFrame parent) {
        super(parent, "Settings", true);
        setSize(300, 200);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        cpuCheckBox = new JCheckBox("Show CPU Info", true);
        ramCheckBox = new JCheckBox("Show RAM Info", true);
        ssdCheckBox = new JCheckBox("Show SSD Info", true);
        networkCheckBox = new JCheckBox("Show Network Info", true);

        add(cpuCheckBox);
        add(ramCheckBox);
        add(ssdCheckBox);
        add(networkCheckBox);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsAccepted = true;
                setVisible(false);
            }
        });

        add(okButton);
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
}
