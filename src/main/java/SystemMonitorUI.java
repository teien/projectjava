import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
    private JLabel cpuFrequencyLabel;
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

    private HardwareAbstractionLayer hal;
    private OperatingSystem os;
    private CentralProcessor processor;
    private long[] prevTicks;
    private NetworkIF networkIF;
    private long lastDownloadBytes;
    private long lastUploadBytes;
    private long prevAvgFrequency;

    private final boolean showCpu;
    private final boolean showRam;
    private final boolean showSsd;
    private final boolean showNetwork;

    public SystemMonitorUI(boolean showCpu, boolean showRam, boolean showSsd, boolean showNetwork) {
        this.showCpu = showCpu;
        this.showRam = showRam;
        this.showSsd = showSsd;
        this.showNetwork = showNetwork;

        initializeUI();
        initializeSystemInfo();
        startUpdateTimer();

        // Get the size of the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Determine the new location of the window
        int w = this.getSize().width;
        int h = this.getSize().height;
        int x = (dim.width - w);
        int y = 0;

        // Move the window
        this.setLocation(x, y);
    }

    private void initializeUI() {
        setTitle("System Monitor");
        setSize(250, 500);
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

        add(panel);
    }

    private void initializeSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();
        hal = systemInfo.getHardware();
        os = systemInfo.getOperatingSystem();
        processor = hal.getProcessor();
        prevTicks = processor.getSystemCpuLoadTicks();
        prevAvgFrequency = 0;

        List<NetworkIF> networkIFs = systemInfo.getHardware().getNetworkIFs();
        if (!networkIFs.isEmpty()) {
            for (NetworkIF networkIF : networkIFs) {
                if (networkIF.isConnectorPresent()) {
                    this.networkIF = networkIF;
                    break;
                }
            }
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

        if (showCpu) {
            long[] newTicks = processor.getSystemCpuLoadTicks();
            double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
            prevTicks = newTicks;
            cpuUsageLabel.setText(String.format("CPU Usage: %.1f%%", cpuLoad));

            double cpuTemperature = hal.getSensors().getCpuTemperature();
            System.out.println("CPU Temperature: " + cpuTemperature + "°C");
        }

        GlobalMemory memory = hal.getMemory();
        if (showRam) {
            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            long usedMemory = totalMemory - availableMemory;
            ramTotalLabel.setText(String.format("RAM Total: %.2f GiB", totalMemory / 1e9));
            ramInUseLabel.setText(String.format("In Use: %.2f GiB", usedMemory / 1e9));
            ramFreeLabel.setText(String.format("Free: %.2f GiB", availableMemory / 1e9));
        }

        if (showSsd && !hal.getDiskStores().isEmpty()) {
            File diskPartition = new File("C:");
            long totalDisk = diskPartition.getTotalSpace();

            long usableDisk = diskPartition.getFreeSpace();
            long usedDisk = totalDisk - usableDisk;
            ssdTotalLabel.setText(String.format("SSD Total: %.2f GiB", totalDisk / 1e9));
            ssdFreeLabel.setText(String.format("Free: %.2f GiB", usableDisk / 1e9));
            ssdUsedLabel.setText(String.format("Used: %.2f GiB", usedDisk / 1e9));
        }

        if (showNetwork && networkIF != null) {
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
        repaint();
    }

    private String formatUptime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, secs);
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
                        settingsUI.isNetworkSelected()
                );
                ui.setVisible(true);
            }
        });
    }
}
