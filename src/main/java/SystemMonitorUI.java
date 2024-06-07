import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.json.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

public class SystemMonitorUI extends JFrame {

    private static JLabel timeLabel;
    private static JLabel dateLabel;
    private static JLabel weatherLabel;
    private static JLabel kernelLabel;
    private static JLabel uptimeLabel;
    private static JLabel cpuUsageLabel;
    private static JLabel cpuTemperatureLabel;
    private static JLabel cpuNameLabel;
    private static JLabel ramTotalLabel;
    private static JLabel ramInUseLabel;
    private static JLabel ramFreeLabel;
    private static JLabel ssdTotalLabel;
    private static JLabel ssdFreeLabel;
    private static JLabel ssdUsedLabel;
    private static JLabel networkIPLabel;
    private static JLabel networkDownloadSpeedLabel;
    private static JLabel networkUploadSpeedLabel;
    private static JLabel networkDownloadTotalLabel;
    private static JLabel networkUploadTotalLabel;
    private static JLabel processLabel;

    private static JLabel gpuTemperatureLabel;
    private static JLabel gpuUsageLabel;
    private static JLabel gpuNameLabel;
    private static JLabel SYSTEMLabel;
    private static JLabel CPULabel;
    private static JLabel GPULabel;
    private static JLabel RAMLabel;
    private static JLabel SSDLabel;
    private static JLabel NETWORKLabel;
    private static JLabel PROCESSLabel;

    private static String fontType1;
    private static int fontSize1;

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
    private final boolean showWeather;
    private final boolean showGpu;
    private final boolean showProcess;

    private final ScheduledExecutorService systemInfoExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService weatherUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService checkUpdateNet = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService checkUpdateWeather = Executors.newSingleThreadScheduledExecutor();
    private static int fontColor1;
    private static int bgColor1;
    private static double opacity1;
    private static int fontColor2;
    private static String fontType2;
    private static int fontSize2;
    private static int bgColor;
    private static double opacity;
    private JPanel panel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    };


    public SystemMonitorUI(boolean showCpu, boolean showRam, boolean showSsd, boolean showNetwork, boolean showWeather, boolean showGpu, boolean showProcess) {
        this.showCpu = showCpu;
        this.showRam = showRam;
        this.showSsd = showSsd;
        this.showNetwork = showNetwork;
        this.showWeather = showWeather;
        this.showGpu = showGpu;
        this.showProcess = showProcess;

        initializeUI();
        initializeSystemInfo();
        startUpdateTimer();
        // Set the location of the window to the top right corner of the screen
       updateLocation();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            systemInfoExecutor.shutdown();
            weatherUpdateExecutor.shutdown();
            checkUpdateNet.shutdown();
            checkUpdateWeather.shutdown();
        }));

    }

    private void updateLocation() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width -getWidth();
        int y = 0;
        setLocation(x, y);
    }

    public void initializeUI() {
        setTitle("System Monitor");
        setMaximumSize(new Dimension(1000, getMaximumSize().height));
        setMinimumSize(new Dimension(250, 900));
        setResizable(false);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setBackground(new Color(0, 0, 0, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        setBackgroundColorUpdate();
        setOpacityUpdate();


        timeLabel = createLabel("HH:mm", 60);
        dateLabel = createLabel("EEE, dd MMM yyyy", 20);
        weatherLabel = createLabel("Weather: --", 14);
        BufferedImage blankImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = blankImage.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        g2d.fillRect(0, 0, 50, 50);
        g2d.dispose();
        ImageIcon transparentIcon = new ImageIcon(blankImage);
        weatherLabel.setIcon(transparentIcon);
        kernelLabel = createLabel("Computer:-- ", 14);
        uptimeLabel = createLabel("Uptime: 0h 0m 0s", 14);
        cpuUsageLabel = createLabel("CPU Usage: 0%", 14);
        cpuTemperatureLabel = createLabel("CPU Temperature: 0°C", 14);
        cpuNameLabel = createLabel("CPU: --", 16);
        processLabel = createLabel("Process: --", 16);

        gpuNameLabel = createLabel("GPU: --", 16);
        gpuTemperatureLabel = createLabel("GPU Temperature: 0°C", 14);
        gpuUsageLabel = createLabel("GPU Usage: 0%", 14);

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
        SYSTEMLabel = createSeparator("SYSTEM");
        CPULabel = createSeparator("CPU");
        GPULabel = createSeparator("GPU");
        RAMLabel = createSeparator("RAM");
        SSDLabel = createSeparator("STORAGE");
        NETWORKLabel = createSeparator("NETWORK");
        PROCESSLabel = createSeparator("PROCESS");

        panel.add(timeLabel);
        panel.add(dateLabel);
        if (showWeather) {
            panel.add(weatherLabel);
            panel.add(SYSTEMLabel);
        }

        panel.add(kernelLabel);
        panel.add(uptimeLabel);
        if (showCpu) {
            panel.add(CPULabel);
            panel.add(cpuNameLabel);
            panel.add(cpuUsageLabel);
            panel.add(cpuTemperatureLabel);
        }
        if (showProcess) {
            panel.add(PROCESSLabel);
            panel.add(processLabel);
        }

        if (showGpu) {
            panel.add(GPULabel);
            panel.add(gpuNameLabel);
            panel.add(gpuTemperatureLabel);
            panel.add(gpuUsageLabel);
        }

        if (showRam) {
            panel.add(RAMLabel);
            panel.add(ramTotalLabel);
            panel.add(ramInUseLabel);
            panel.add(ramFreeLabel);
        }

        if (showSsd) {
            panel.add(SSDLabel);
            panel.add(ssdTotalLabel);
            panel.add(ssdFreeLabel);
            panel.add(ssdUsedLabel);
        }

        if (showNetwork) {
            panel.add(NETWORKLabel);
            panel.add(networkIPLabel);
            panel.add(networkDownloadSpeedLabel);
            panel.add(networkUploadSpeedLabel);
            panel.add(networkDownloadTotalLabel);
            panel.add(networkUploadTotalLabel);
        }

        add(panel);
    }

    private void setBackgroundColorUpdate() {
        JSONObject settings = SettingsLogger.loadSettings();
        if (!settings.isEmpty()) {
            int bgColor = settings.getInt("bgColor");
            Color colorWithAlpha = new Color(bgColor, true);
            panel.setBackground(colorWithAlpha);
        }
    }



    private static WinDef.HWND getHWnd(Component w) {
        WinDef.HWND hwnd = new WinDef.HWND();
        hwnd.setPointer(Native.getComponentPointer(w));
        return hwnd;
    }
    private void initializeSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();
        hal = systemInfo.getHardware();
        os = systemInfo.getOperatingSystem();
        processor = hal.getProcessor();
        prevTicks = processor.getSystemCpuLoadTicks();
        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        if (!networkIFs.isEmpty()) {
            for (NetworkIF networkIF : networkIFs) {
                if (networkIF.isConnectorPresent() && networkIF.getIPv4addr().length > 0 && networkIF.getSpeed() > 0 && networkIF.getIfOperStatus().equals(NetworkIF.IfOperStatus.UP)) {
                    this.networkIF = networkIF;
                    break;
                }
            }
        }
    }

    private void startUpdateTimer() {

        // Update weather if first time can not be done because of network issues
        if ((showNetwork) && (networkIPLabel.getText().equals("IP: --"))) {
            checkUpdateNet.scheduleAtFixedRate(() -> {
                SwingUtilities.invokeLater(this::initializeSystemInfo);
                if (!networkIPLabel.getText().equals("IP: --")) {
                    checkUpdateNet.shutdown();
                }
            }, 20, 20, TimeUnit.SECONDS);
        }
       // Update weather if first time can not be done because of network issues
        if ((showWeather) && (!updateWeatherInfo())) {
            checkUpdateWeather.scheduleAtFixedRate(() -> {
                SwingUtilities.invokeLater(this::updateWeatherInfo) ;
                if(updateWeatherInfo()) {
                    checkUpdateWeather.shutdown();
                }
            }, 20, 10, TimeUnit.SECONDS);
        }
        if (showWeather) {
            weatherUpdateExecutor.scheduleAtFixedRate(this::updateWeatherInfo, 0, 30, TimeUnit.MINUTES);
        }
        systemInfoExecutor.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(this::updateSystemInfo );
        }, 0, 1, TimeUnit.SECONDS);

    }

    private JLabel createLabel(String text, int fontSize1) {
        JLabel label = new JLabel(text);
        JSONObject settings = SettingsLogger.loadSettings();

        if (!settings.isEmpty()) {
            fontType1 = settings.getString("fontType1");
            fontSize1 = settings.getInt("fontSize1");
            fontColor1 = settings.optInt("fontColor1", Color.WHITE.getRGB());
        } else {
            fontType1 = "SansSerif";
            fontSize1 = 14;
        }

        label.setFont(new Font(fontType1, Font.PLAIN, fontSize1));
        label.setForeground(new Color(fontColor1));
        return label;
    }
    void updateSetting(){
        setFontUpdate(timeLabel,dateLabel,weatherLabel,kernelLabel,uptimeLabel,cpuUsageLabel,cpuTemperatureLabel,cpuNameLabel,ramTotalLabel,ramInUseLabel,ramFreeLabel,ssdTotalLabel,ssdFreeLabel,ssdUsedLabel,networkIPLabel,networkDownloadSpeedLabel,networkUploadSpeedLabel,networkDownloadTotalLabel,networkUploadTotalLabel,gpuTemperatureLabel,gpuUsageLabel,gpuNameLabel);
        setFontColorUpdate(timeLabel,dateLabel,weatherLabel,kernelLabel,uptimeLabel,cpuUsageLabel,cpuTemperatureLabel,cpuNameLabel,ramTotalLabel,ramInUseLabel,ramFreeLabel,ssdTotalLabel,ssdFreeLabel,ssdUsedLabel,networkIPLabel,networkDownloadSpeedLabel,networkUploadSpeedLabel,networkDownloadTotalLabel,networkUploadTotalLabel,gpuTemperatureLabel,gpuUsageLabel,gpuNameLabel);
        setFontUpdateTitle(SYSTEMLabel,CPULabel,GPULabel,RAMLabel,SSDLabel,NETWORKLabel);
        setOpacityUpdate();
        setBackgroundColorUpdate();


    }

    void setOpacityUpdate(){
        JSONObject settings = SettingsLogger.loadSettings();
        if (!settings.isEmpty()) {
            double opacity = settings.getDouble("opacity");
            this.setOpacity((float) opacity);  // Fixed here
        }
    }
    static void setFontUpdate(JLabel... labels){
        JSONObject settings = SettingsLogger.loadSettings();
        if (!settings.isEmpty()) {
            fontType1 = settings.getString("fontType1");
            fontSize1 = settings.getInt("fontSize1");
            fontColor1 = settings.optInt("fontColor1", Color.WHITE.getRGB());
        } else {
            fontType1 = "SansSerif";
            fontSize1 = 14;
        }

        for (JLabel label : labels) {
            label.setFont(new Font(fontType1, Font.PLAIN, fontSize1));
        }
    }
    static void setFontUpdateTitle(JLabel... labels){
        JSONObject settings = SettingsLogger.loadSettings();
        if (!settings.isEmpty()) {
            fontType2 = settings.getString("fontType2");
            fontSize2 = settings.getInt("fontSize2");
            fontColor2 = settings.optInt("fontColor2", Color.WHITE.getRGB());
        } else {
            fontType2 = "SansSerif";
            fontSize2 = 14;
        }

        for (JLabel label : labels) {
            String fontColorHex = String.format("#%06X", (0xFFFFFF & fontColor2));
            label.setText("<html><div style='font-weight: bold;'>"
                    + getPlainText(label) + "<div style='height: 2px; width:1000px;background-color: " + fontColorHex + ";'></div></div></html>");
            label.setFont(new Font(fontType2, Font.PLAIN, fontSize2));
            label.setForeground(new Color(fontColor2));
        }
    }
    private static String getPlainText(JLabel label) {
        String htmlText = label.getText();
        return htmlText.replaceAll("<[^>]*>", "");
    }
    static void setFontColorUpdate(JLabel... labels){
        JSONObject settings = SettingsLogger.loadSettings();
        fontColor1 = settings.optInt("fontColor1", Color.WHITE.getRGB());
        for (JLabel label : labels) {
            label.setForeground(new Color(fontColor1));
        }
    }

    private JLabel createSeparator(String text) {
        JSONObject settings = SettingsLogger.loadSettings();
        if (!settings.isEmpty()) {
            fontType2 = settings.getString("fontType2");
            fontSize2 = settings.getInt("fontSize2");
            fontColor2 = settings.optInt("fontColor2", Color.WHITE.getRGB());
        } else {
            fontType2 = "SansSerif";
            fontSize2 = 14;
        }
        String fontColorHex = String.format("#%06X", (0xFFFFFF & fontColor2));
        String htmlText = "<html><div style='font-weight: bold;'>"
                + text + "<div style='height: 2px; width:1000px;background-color: " + fontColorHex + ";'></div></div></html>";
        JLabel separator = new JLabel(htmlText);
        separator.setFont(new Font(fontType2, Font.PLAIN, fontSize2));
        separator.setForeground(new Color(fontColor2));
        separator.setBorder(new EmptyBorder(5, 0, 5, 0));
        return separator;
    }

    private void updateSystemInfo() {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");
            Date now = new Date();
            timeLabel.setText(timeFormat.format(now));
            dateLabel.setText(dateFormat.format(now));

            String computerName = System.getenv("COMPUTERNAME");
            String osName = System.getProperty("os.name").toLowerCase();
            kernelLabel.setText("Computer: " + computerName + " (" + osName + ")");
            uptimeLabel.setText("Uptime: " + formatUptime(os.getSystemUptime()));

            ServiceManager.HwInfo hwInfo = ServiceManager.HwInfo.getHwInfo();
            if (showCpu) {
                long[] newTicks = processor.getSystemCpuLoadTicks();
                double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
                prevTicks = newTicks;
                cpuUsageLabel.setText(String.format("CPU Usage: %.1f%%", cpuLoad));
                Double cpuTemperature = hwInfo.cpuTemperature();
                cpuTemperatureLabel.setText(String.format("CPU Temperature: %.1f°C", cpuTemperature));
                cpuNameLabel.setText(hwInfo.cpuName()) ;
            }
            if (showGpu) {
                Double gpuTemperature = hwInfo.gpuTemperature();
                Double gpuUsage = hwInfo.gpuUsage();
                gpuTemperatureLabel.setText(String.format("GPU Temperature: %.1f°C", gpuTemperature));
                gpuUsageLabel.setText(String.format("GPU Usage: %.1f%%", gpuUsage));
                gpuNameLabel.setText(hwInfo.gpuName());
            }

            GlobalMemory memory = hal.getMemory();
            if (showRam) {
                long totalMemory = memory.getTotal();
                long availableMemory = memory.getAvailable();
                long usedMemory = totalMemory - availableMemory;
                ramTotalLabel.setText(String.format("RAM Total: %.2f GiB", totalMemory / 1e9));
                ramInUseLabel.setText(String.format("In Use: %.2f GiB", usedMemory / 1e9) +"    ("+String.format("%.1f%%", (double) usedMemory / totalMemory * 100)+")");
                ramFreeLabel.setText(String.format("Free: %.2f GiB", availableMemory / 1e9)+"   ("+String.format("%.1f%%", (double) availableMemory / totalMemory * 100)+")");
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
                double downloadSpeedMbps = (double) (downloadBytes - lastDownloadBytes) * 8 / 1024 / 1024;
                double uploadSpeedMbps = (double) (uploadBytes - lastUploadBytes) * 8 / 1024 / 1024;
                lastDownloadBytes = downloadBytes;
                lastUploadBytes = uploadBytes;

                networkIPLabel.setText(networkIF.getIPv4addr().length > 0 ? "IP: " + networkIF.getIPv4addr()[0] : "IP: --");
                networkDownloadSpeedLabel.setText(String.format("Download Speed: %.1f Mbps", downloadSpeedMbps));
                networkUploadSpeedLabel.setText(String.format("Upload Speed: %.1f Mbps", uploadSpeedMbps));
                networkDownloadTotalLabel.setText(String.format("Download Total: %.2f MiB", downloadBytes / 1e6));
                networkUploadTotalLabel.setText(String.format("Upload Total: %.2f MiB", uploadBytes / 1e6));
            }
            if (SettingsPanel.checkSettings) {
                updateSetting();
                SettingsPanel.checkSettings = Boolean.FALSE;
            }
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean updateWeatherInfo() {
        try {
            WeatherByIP.getWeatherInfo().thenAccept(weatherInfo -> {
                try {
                    String weatherIcon = weatherInfo.getIconCode();
                    ImageIcon icon = new ImageIcon(new URL("http://openweathermap.org/img/w/" + weatherIcon + ".png"));

                    SwingUtilities.invokeLater(() -> {
                        weatherLabel.setIcon(icon);
                        weatherLabel.setText("   " + weatherInfo.getTemperature() + "°C" + "    " + weatherInfo.getCity());
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        weatherLabel.setText("Weather: --");
                    });
                }
            }).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    weatherLabel.setText("Weather: --");
                });
                ex.printStackTrace();
                return null;
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
            //settingsUI.setVisible(true);
            //if (settingsUI.isSettingsAccepted()) {
                SystemMonitorUI ui = new SystemMonitorUI(
                        settingsUI.isCpuSelected(),
                        settingsUI.isRamSelected(),
                        settingsUI.isSsdSelected(),
                        settingsUI.isNetworkSelected(),
                        settingsUI.isWeatherSelected(),
                        settingsUI.isGpuSelected(),
                        settingsUI.isProcessSelected()
                );
                ui.setVisible(true);
                SystemTrayApp systemTrayApp = new SystemTrayApp();
                WinDef.HWND hwnd = getHWnd(ui);
                int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
                exStyle |= WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
                User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);

            //}
        });


    }

}
