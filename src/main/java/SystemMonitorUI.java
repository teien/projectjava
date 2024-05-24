/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.doanjava2;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

public class SystemMonitorUI extends JFrame {

    private JLabel timeLabel;
    private JLabel dateLabel;
    private JLabel kernelLabel;
    private JLabel uptimeLabel;
    private JLabel cpuUsageLabel;
    private JLabel cpuTemperatureLabel;
    private JLabel ramTotalLabel;
    private JLabel ramInUseLabel;
    private JLabel ramFreeLabel;
    private JLabel ssdTotalLabel;
    private JLabel ssdFreeLabel;
    private JLabel ssdUsedLabel;
    private JLabel networkIPLabel;
    private JLabel networkDownloadSpeedLabel;
    private JLabel networkUploadSpeedLabel;
    private JLabel networkDownloadTotalLabel;
    private JLabel networkUploadTotalLabel;
    private JTextPane processesTextPane;

    private HardwareAbstractionLayer hal;
    private OperatingSystem os;
    private CentralProcessor processor;
    private long[] prevTicks;
    private NetworkIF networkIF;
    private long lastDownloadBytes;
    private long lastUploadBytes;

    private final boolean showCpu;
    private final boolean showRam;
    private final boolean showSsd;
    private final boolean showNetwork;
    private final boolean showProcesses;

    public SystemMonitorUI(boolean showCpu, boolean showRam, boolean showSsd, boolean showNetwork, boolean showProcesses) {
        this.showCpu = showCpu;
        this.showRam = showRam;
        this.showSsd = showSsd;
        this.showNetwork = showNetwork;
        this.showProcesses = showProcesses;

        initializeUI();
        initializeSystemInfo();
        startUpdateTimer();

        // Lấy kích thước của màn hình
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Xác định vị trí mới của cửa sổ
        int w = this.getSize().width;
        int h = this.getSize().height;
        int x = (dim.width - w);
        int y = 0;

        // Di chuyển cửa sổ
        this.setLocation(x, y);
    }

    private void initializeUI() {
        setTitle("System Monitor");
        setSize(950, 500);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(0, 0, 0, 0));

        timeLabel = createLabel("HH:mm", 36);
        dateLabel = createLabel("EEE, dd MMM yyyy", 18);
        kernelLabel = createLabel("", 14);
        uptimeLabel = createLabel("Uptime: 0h 0m 0s", 14);
        cpuUsageLabel = createLabel("CPU Usage: 0%", 14);

        ramTotalLabel = createLabel("RAM Total: 0 GiB", 14);
        ramInUseLabel = createLabel("In Use: 0 GiB", 14);
        ramFreeLabel = createLabel("Free: 0 GiB", 14);
        ssdTotalLabel = createLabel("SSD Total: 0 GiB", 14);
        ssdFreeLabel = createLabel("Free: 0 GiB", 14);
        ssdUsedLabel = createLabel("Used: 0 GiB", 14);
        networkIPLabel = createLabel("IP: --", 14);
        networkDownloadSpeedLabel = createLabel("Download Speed: 0 kB/s", 14);
        networkUploadSpeedLabel = createLabel("Upload Speed: 0 kB/s", 14);
        networkDownloadTotalLabel = createLabel("Download Total: 0 MiB", 14);
        networkUploadTotalLabel = createLabel("Upload Total: 0 KiB", 14);
        processesTextPane = createTextPane("Processes", 14);

        panel.add(timeLabel);
        panel.add(dateLabel);
        panel.add(createSeparator());
        panel.add(kernelLabel);
        panel.add(uptimeLabel);
        panel.add(createSeparator());
        if (showCpu) {
            panel.add(cpuUsageLabel);
        }
        panel.add(createSeparator());
        if (showRam) {
            panel.add(ramTotalLabel);
            panel.add(ramInUseLabel);
            panel.add(ramFreeLabel);
        }
        panel.add(createSeparator());
        if (showSsd) {
            panel.add(ssdTotalLabel);
            panel.add(ssdFreeLabel);
            panel.add(ssdUsedLabel);
        }
        panel.add(createSeparator());

        if (showNetwork) {
            panel.add(networkIPLabel);
            panel.add(networkDownloadSpeedLabel);
            panel.add(networkUploadSpeedLabel);
            panel.add(networkDownloadTotalLabel);
            panel.add(networkUploadTotalLabel);
        }
        panel.add(createSeparator());

        if (showProcesses) {
            JScrollPane scrollPane = new JScrollPane(processesTextPane);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(null);
            panel.add(scrollPane);
        }

        add(panel);
    }

   private JTextPane createTextPane(String text, int fontSize) {
    JTextPane textPane = new JTextPane();
    textPane.setContentType("text/plain");
    textPane.setText(text);
    textPane.setFont(new Font("SansSerif", Font.BOLD, fontSize));
    textPane.setForeground(Color.GREEN);
    textPane.setEditable(false);
    textPane.setOpaque(false);
    textPane.setBackground(new Color(0, 0, 0, 0));
    StyledDocument doc = textPane.getStyledDocument();
    SimpleAttributeSet padding = new SimpleAttributeSet();
    StyleConstants.setLeftIndent(padding, 24); 
    doc.setParagraphAttributes(0, doc.getLength(), padding, false);

    return textPane;
}



    private void initializeSystemInfo() {
        try {
            SystemInfo systemInfo = new SystemInfo();
            hal = systemInfo.getHardware();
            os = systemInfo.getOperatingSystem();
            processor = hal.getProcessor();
            prevTicks = processor.getSystemCpuLoadTicks();

            List<NetworkIF> networkIFs = hal.getNetworkIFs();
            if (!networkIFs.isEmpty()) {
                for (NetworkIF networkIF : networkIFs) {
                    if (networkIF.isConnectorPresent()) {
                        this.networkIF = networkIF;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            showError("Error initializing system information: " + e.getMessage());
        }
    }

    private void startUpdateTimer() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateSystemInfo());
            }
        }, 0, 1000);
    }

    private JLabel createLabel(String text, int fontSize) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        label.setForeground(Color.GREEN);
        return label;
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(Color.GREEN);
        return separator;
    }

    private void updateSystemInfo() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");
        Date now = new Date();
        timeLabel.setText(timeFormat.format(now));
        dateLabel.setText(dateFormat.format(now));

        String computerName = System.getenv("COMPUTERNAME");
        String osName = System.getProperty("os.name").toLowerCase();
        kernelLabel.setText("Computer: " + computerName + " (" + osName + ")");
        uptimeLabel.setText("Uptime: " + formatUptime(os.getSystemUptime()));

        try {
            if (showCpu) {
                updateCpuInfo();
            }

            if (showRam) {
                updateRamInfo();
            }

            if (showSsd) {
                updateSsdInfo();
            }

            if (showProcesses) {
                updateProcessInfo();
            }

            if (showNetwork && networkIF != null) {
                updateNetworkInfo();
            }
        } catch (Exception e) {
            showError("Error updating system information: " + e.getMessage());
        }

        repaint();
    }

    private void updateCpuInfo() {
        long[] newTicks = processor.getSystemCpuLoadTicks();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = newTicks;
        cpuUsageLabel.setText(String.format("CPU Usage: %.1f%%", cpuLoad));

//        double cpuTemperature = hal.getSensors().getCpuTemperature();
//        cpuTemperatureLabel.setText(String.format("CPU Temp: %.1f°C", cpuTemperature));
    }

    private void updateRamInfo() {
        GlobalMemory memory = hal.getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        ramTotalLabel.setText(String.format("RAM Total: %.2f GiB", totalMemory / 1e9));
        ramInUseLabel.setText(String.format("In Use: %.2f GiB", usedMemory / 1e9));
        ramFreeLabel.setText(String.format("Free: %.2f GiB", availableMemory / 1e9));
    }

    private void updateSsdInfo() {
        if (!hal.getDiskStores().isEmpty()) {
            File diskPartition = new File("C:");
            long totalDisk = diskPartition.getTotalSpace();
            long usableDisk = diskPartition.getFreeSpace();
            long usedDisk = totalDisk - usableDisk;
            ssdTotalLabel.setText(String.format("SSD Total: %.2f GiB", totalDisk / 1e9));
            ssdFreeLabel.setText(String.format("Free: %.2f GiB", usableDisk / 1e9));
            ssdUsedLabel.setText(String.format("Used: %.2f GiB", usedDisk / 1e9));
        }
    }

    private void updateProcessInfo() throws BadLocationException {
    List<oshi.software.os.OSProcess> processes = os.getProcesses(
            OperatingSystem.ProcessFiltering.ALL_PROCESSES,
            OperatingSystem.ProcessSorting.CPU_DESC,
            6
    );

    // Loại bỏ các tiến trình idle
    processes = processes.stream()
            .filter(p -> !p.getName().equalsIgnoreCase("idle"))
            .collect(Collectors.toList());

    // Xóa nội dung hiện tại của JTextPane
    processesTextPane.setText("");

    // Lấy StyledDocument từ JTextPane
    StyledDocument doc = processesTextPane.getStyledDocument();

    Style style = doc.addStyle("processStyle", null);
    StyleConstants.setBold(style, true); // Định dạng chữ đậm
    StyleConstants.setForeground(style, Color.GREEN); // Định dạng màu chữ

    Style normalStyle = doc.addStyle("normalStyle", null);
    StyleConstants.setForeground(normalStyle, Color.GREEN); // Định dạng màu chữ

    for (oshi.software.os.OSProcess p : processes) {
        String processName = p.getName();
        String capitalizedProcessName = processName.substring(0, 1).toUpperCase() + processName.substring(1);
        String processInfo = String.format(" %.2f%%\n", 100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime());

        // Thêm văn bản được định dạng vào StyledDocument
        doc.insertString(doc.getLength(), capitalizedProcessName, style);
        doc.insertString(doc.getLength(), processInfo, normalStyle);
    }
}

    private void updateNetworkInfo() {
        networkIF.updateAttributes();
        long downloadBytes = networkIF.getBytesRecv();
        long uploadBytes = networkIF.getBytesSent();
        long downloadSpeed = (downloadBytes - lastDownloadBytes) / 1024; // in KiB/s
        long uploadSpeed = (uploadBytes - lastUploadBytes) / 1024; // in KiB/s
        lastDownloadBytes = downloadBytes;
        lastUploadBytes = uploadBytes;

        networkIPLabel.setText(networkIF.getIPv4addr().length > 0 ? "IP: " + networkIF.getIPv4addr()[0] : "IP: --");
        networkDownloadSpeedLabel.setText(String.format("Download Speed: %d kB/s", downloadSpeed));
        networkUploadSpeedLabel.setText(String.format("Upload Speed: %d kB/s", uploadSpeed));
        networkDownloadTotalLabel.setText(String.format("Download Total: %.2f MiB", downloadBytes / 1e6));
        networkUploadTotalLabel.setText(String.format("Upload Total: %.2f KiB", uploadBytes / 1e3));
    }

    private String formatUptime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, secs);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame dummyFrame = new JFrame();
            SettingUI settingsUI = new SettingUI(dummyFrame);
            settingsUI.setVisible(true);

            if (settingsUI.isSettingsAccepted()) {
                SystemMonitorUI ui = new SystemMonitorUI(
                        settingsUI.isCpuSelected(),
                        settingsUI.isRamSelected(),
                        settingsUI.isSsdSelected(),
                        settingsUI.isNetworkSelected(),
                        settingsUI.isProcessesSelected()
                );
                ui.setVisible(true);
            }
        });
    }

   
}
