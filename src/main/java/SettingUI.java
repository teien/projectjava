/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.doanjava2;

/**
 *
 * @author bogia
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class SettingUI extends JDialog {

    private JCheckBox selectAllCheckBox;
    private JCheckBox cpuCheckBox;
    private JCheckBox ramCheckBox;
    private JCheckBox ssdCheckBox;
    private JCheckBox networkCheckBox;
    private JCheckBox processesCheckBox;
    private boolean settingsAccepted;

    public SettingUI(JFrame parent) {
        super(parent, "Settings", true);
        setSize(300, 250);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(6, 1));

        selectAllCheckBox = new JCheckBox("Select All");
        cpuCheckBox = new JCheckBox("CPU");
        ramCheckBox = new JCheckBox("RAM");
        ssdCheckBox = new JCheckBox("SSD");
        networkCheckBox = new JCheckBox("Network");
        processesCheckBox = new JCheckBox("Processes");

        panel.add(selectAllCheckBox);
        panel.add(cpuCheckBox);
        panel.add(ramCheckBox);
        panel.add(ssdCheckBox);
        panel.add(networkCheckBox);
        panel.add(processesCheckBox);

        selectAllCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isSelected = selectAllCheckBox.isSelected();
                cpuCheckBox.setSelected(isSelected);
                ramCheckBox.setSelected(isSelected);
                ssdCheckBox.setSelected(isSelected);
                networkCheckBox.setSelected(isSelected);
                processesCheckBox.setSelected(isSelected);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsAccepted = true;
                dispose();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsAccepted = false;
                dispose();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
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

    public boolean isProcessesSelected() {
        return processesCheckBox.isSelected();
    }
}
