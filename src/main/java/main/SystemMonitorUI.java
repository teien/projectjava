package main;

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
import settings.SettingUI;
import settings.SettingsLogger;
import settings.SettingsPanel;
import system.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private static JLabel PROCESSESLabel;
    static final JLabel[] processListLabel = new JLabel[5];

    static {
        for (int i = 0; i < 5; i++) {
            processListLabel[i] = new JLabel();
        }
    }

    private static String fontType1;
    private static HardwareAbstractionLayer hal;
    private static OperatingSystem os;
    private static NetworkIF networkIF;
    private long lastDownloadBytes;
    private long lastUploadBytes;

    private final boolean showCpu;
    private final boolean showRam;
    private final boolean showSsd;
    private final boolean showNetwork;
    private final boolean showWeather;
    private final boolean showGpu;
    private final boolean showProcess;

    private static final Map<Integer, Long> previousTimes = new HashMap<>();
    private static final Map<Integer, Long> previousUpTimes = new HashMap<>();
    private final ScheduledExecutorService systemInfoExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService weatherUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService checkUpdateNet = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService processExecutor = Executors.newSingleThreadScheduledExecutor();
    private static int fontColor1;
    private static int fontColor2;
    private static String fontType2;
    private static int fontSize2;
    private static JSONObject settings = SettingsLogger.loadSettings();
    private final JPanel panel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    private static int cpuNumber;
    private NetworkMonitor downloadMonitor;
    private NetworkMonitor uploadMonitor;

    public SystemMonitorUI(boolean showCpu, boolean showRam, boolean showSsd, boolean showNetwork, boolean showWeather, boolean showGpu, boolean showProcess) {
        this.showCpu = showCpu;
        this.showRam = showRam;
        this.showSsd = showSsd;
        this.showNetwork = showNetwork;
        this.showWeather = showWeather;
        this.showGpu = showGpu;
        this.showProcess = showProcess;

        initializeSystemInfo();
        initializeUI();
        startUpdateTimer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            systemInfoExecutor.shutdown();
            weatherUpdateExecutor.shutdown();
            checkUpdateNet.shutdown();
            processExecutor.shutdown();
        }));

    }

    public static void initializeSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        //  cpuNumber = processor.getPhysicalProcessorCount() * processor.getPhysicalPackageCount();
        cpuNumber = processor.getLogicalProcessorCount();
        hal = systemInfo.getHardware();
        os = systemInfo.getOperatingSystem();
        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        if (!networkIFs.isEmpty()) {
            for (NetworkIF networkIF : networkIFs) {
                if (networkIF.isConnectorPresent() && networkIF.getIPv4addr().length > 0 && networkIF.getSpeed() > 0 && networkIF.getIfOperStatus().equals(NetworkIF.IfOperStatus.UP)) {
                    SystemMonitorUI.networkIF = networkIF;
                    break;
                }
            }
        }
    }

    public static void restart() {
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            System.out.println("Java bin path: " + javaBin);

            File currentJar = new File(SystemMonitorUI.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            System.out.println("Current JAR path: " + currentJar.getPath());

            if (!currentJar.getName().endsWith(".jar")) {
                System.out.println("The current file is not a JAR file.");
                return;
            }

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-jar");
            command.add(currentJar.getPath());
            System.out.println("Command: " + String.join(" ", command));

            // Khởi chạy quá trình mới
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            System.out.println("Restarting application...");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }


    private void setBackgroundColorUpdate() {

        if (!settings.isEmpty()) {
            int bgColor = settings.getJSONObject("Style").getInt("bgColor");
            Color colorWithAlpha = new Color(bgColor, true);
            panel.setBackground(colorWithAlpha);
        }
    }

    private static WinDef.HWND getHWnd(Component w) {
        WinDef.HWND hwnd = new WinDef.HWND();
        hwnd.setPointer(Native.getComponentPointer(w));
        return hwnd;
    }

    public static void updateWeatherInfo() {
        try {
            CompletableFuture<Boolean> future = WeatherByIP.getWeatherInfo()
                    .thenApply(weatherInfo -> {
                        if (weatherInfo == null) {
                            SwingUtilities.invokeLater(() -> weatherLabel.setText("Weather: --"));
                            return false;
                        }
                        try {
                            String weatherIcon = weatherInfo.getIconCode();
                            ImageIcon icon = new ImageIcon(new URL("http://openweathermap.org/img/w/" + weatherIcon + ".png"));
                            SwingUtilities.invokeLater(() -> {
                                weatherLabel.setIcon(icon);
                                weatherLabel.setText("   " + weatherInfo.getTemperature() + "°C" + "    " + weatherInfo.getCity());
                            });
                            return true;
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(() -> weatherLabel.setText("Weather: --"));
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> weatherLabel.setText("Weather: --"));
                       System.out.println("Weather update failure");
                        return false;
                    });
            boolean result = future.join();
            System.out.println("Weather update " + (result ? "success" : "failure"));
        } catch (Exception e) {
            System.out.println("Weather update failure");
        }
    }

    private void startUpdateTimer() {
        // Update weather if first time can not be done because of network issues
        if ((showNetwork) && (networkIPLabel.getText().equals(" IP: --"))) {
            checkUpdateNet.scheduleAtFixedRate(() -> {
              initializeSystemInfo();
                updateWeatherInfo();
                if (!networkIPLabel.getText().equals(" IP: --")) {
                    checkUpdateNet.shutdown();
                }
            }, 20, 10, TimeUnit.SECONDS);
        }
        if (showWeather) {
            weatherUpdateExecutor.scheduleAtFixedRate(SystemMonitorUI::updateWeatherInfo, 0, 30, TimeUnit.MINUTES);
        }
        systemInfoExecutor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::updateSystemInfo), 0, 1, TimeUnit.SECONDS);
        ProcessMonitor pm = new ProcessMonitor(os, previousTimes, previousUpTimes, cpuNumber, processListLabel);

        processExecutor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(() -> {
            if (showProcess) {
                pm.printProcesses();
            }
        }), 0, 3 / 2, TimeUnit.SECONDS);

    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        fontType1 = settings.getJSONObject("Style").getString("fontType1");
        int fontSize1 = settings.getJSONObject("Style").getInt("fontSize1");
        fontColor1 = settings.getJSONObject("Style").optInt("fontColor1", Color.WHITE.getRGB());
        if (Objects.equals(label.getText(), "HH:mm")) {
            label.setFont(new Font(fontType1, Font.PLAIN, 60));
        } else if (Objects.equals(label.getText(), " EEE, dd MMM yyyy")) {
            label.setFont(new Font(fontType1, Font.PLAIN, 20));
        } else {
            label.setFont(new Font(fontType1, Font.PLAIN, fontSize1));
        }
        label.setForeground(new Color(fontColor1));
        return label;
    }

    void updateSetting() {
        settings = SettingsLogger.loadSettings();
        setFontUpdate(processListLabel[0], processListLabel[1], processListLabel[2], processListLabel[3],processLabel, timeLabel,dateLabel, weatherLabel, kernelLabel, uptimeLabel, cpuUsageLabel, cpuTemperatureLabel, cpuNameLabel, ramTotalLabel, ramInUseLabel, ramFreeLabel, ssdTotalLabel, ssdFreeLabel, ssdUsedLabel, networkIPLabel, networkDownloadSpeedLabel, networkUploadSpeedLabel, networkDownloadTotalLabel, networkUploadTotalLabel, gpuTemperatureLabel, gpuUsageLabel, gpuNameLabel);
        setFontUpdateTitle(PROCESSESLabel, SYSTEMLabel, CPULabel, GPULabel, RAMLabel, SSDLabel, NETWORKLabel);
        setOpacityUpdate();
        setBackgroundColorUpdate();
        updateLocationScreen();
        initializeSystemInfo();
    }

    void updateLocationScreen() {
        int w = settings.getJSONObject("Screen").getInt("width");
        int h = settings.getJSONObject("Screen").getInt("height");
        int xc = settings.getJSONObject("Screen").getInt("xc");
        int yc = settings.getJSONObject("Screen").getInt("yc");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - w + xc;
        setLocation(x, yc);
        setSize(new Dimension(w, h));
    }
    void setOpacityUpdate() {

        if (!settings.isEmpty()) {
            double opacity = settings.getJSONObject("Style").getDouble("opacity");
            this.setOpacity((float) opacity);
        }
    }

    static void setFontUpdate(JLabel... labels) {

        fontType1 = settings.getJSONObject("Style").getString("fontType1");
        int fontSize1 = settings.getJSONObject("Style").getInt("fontSize1");
        fontColor1 = settings.getJSONObject("Style").optInt("fontColor1", Color.WHITE.getRGB());
        for (JLabel label : labels) {
            label.setFont(new Font(fontType1, Font.PLAIN, fontSize1));
            label.setForeground(new Color(fontColor1));
            if (Objects.equals(label.getName(), "timeLabel")) {
                label.setFont(new Font(fontType1, Font.PLAIN, 60));
                label.setForeground(new Color(fontColor1));
            }
            if (Objects.equals(label.getName(), "dateLabel")) {
                label.setFont(new Font(fontType1, Font.PLAIN, 20));
                label.setForeground(new Color(fontColor1));
            }
        }
    }

    static void setFontUpdateTitle(JLabel... labels) {

        fontType2 = settings.getJSONObject("Style").getString("fontType2");
        fontSize2 = settings.getJSONObject("Style").getInt("fontSize2");
        fontColor2 = settings.getJSONObject("Style").optInt("fontColor2", Color.WHITE.getRGB());
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

    private JLabel createSeparator(String text) {

        fontType2 = settings.getJSONObject("Style").getString("fontType2");
        fontSize2 = settings.getJSONObject("Style").getInt("fontSize2");
        fontColor2 = settings.getJSONObject("Style").optInt("fontColor2", Color.WHITE.getRGB());
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
           String timeStr = timeFormat.format(now);
           String dateStr = dateFormat.format(now);

           SwingUtilities.invokeLater(() -> {
               timeLabel.setText(timeStr);
               dateLabel.setText(dateStr);
           });

           String computerName = System.getenv("COMPUTERNAME");
           String osName = System.getProperty("os.name").toLowerCase();
           String kernelInfo = " Computer: " + computerName + " (" + osName + ")";
           String uptimeInfo = " Uptime: " + formatUptime(os.getSystemUptime());

           ServiceManager.HwInfo hwInfo = ServiceManager.HwInfo.getHwInfo();

           String cpuUsageInfo = null;
           String cpuTemperatureInfo = null;
           String cpuNameInfo = null;
           if (showCpu) {
               Double cpuLoad = hwInfo.cpuUsage();
               cpuUsageInfo = String.format(" CPU Usage: %15.1f %%", cpuLoad);
               Double cpuTemperature = hwInfo.cpuTemperature();
               cpuTemperatureInfo = String.format(" CPU Temperature: %9.1f°C", cpuTemperature);
               cpuNameInfo = " " + hwInfo.cpuName();
           }

           String gpuTemperatureInfo = null;
           String gpuUsageInfo = null;
           String gpuNameInfo = null;
           if (showGpu) {
               Double gpuTemperature = hwInfo.gpuTemperature();
               Double gpuUsage = hwInfo.gpuUsage();
               gpuTemperatureInfo = String.format(" GPU Temperature: %9.1f°C", gpuTemperature);
               gpuUsageInfo = String.format(" GPU Usage: %15.1f %%", gpuUsage);
               gpuNameInfo = " " + hwInfo.gpuName();
           }

           GlobalMemory memory = hal.getMemory();
           String ramTotalInfo = null;
           String ramInUseInfo = null;
           String ramFreeInfo = null;
           if (showRam) {
               long totalMemory = memory.getTotal();
               long availableMemory = memory.getAvailable();
               long usedMemory = totalMemory - availableMemory;
               ramTotalInfo = String.format(" RAM Total:  %12.2f GiB", totalMemory / 1e9);
               ramInUseInfo = String.format(" In Use:  %6.1f%%", (double) usedMemory / totalMemory * 100) + String.format("%8.2f GiB", usedMemory / 1e9);
               ramFreeInfo = String.format(" Free:  %8.1f%%", (double) availableMemory / totalMemory * 100) + String.format("%8.2f GiB", availableMemory / 1e9);
           }

           String ssdTotalInfo = null;
           String ssdFreeInfo = null;
           String ssdUsedInfo = null;
           if (showSsd) {
               String diskName = settings.getString("diskName");
               Path path = Paths.get(diskName);
               FileStore fileStore = Files.getFileStore(path);
               long totalDisk = fileStore.getTotalSpace();
               long usableDisk = fileStore.getUsableSpace();
               long usedDisk = totalDisk - usableDisk;
               ssdTotalInfo = String.format(" Total: %17.2f GiB", totalDisk / 1e9);
               ssdFreeInfo = String.format(" Free: %18.2f GiB", usableDisk / 1e9);
               ssdUsedInfo = String.format(" Used: %18.2f GiB", usedDisk / 1e9);
           }

           String networkIPInfo = null;
           String networkDownloadSpeedInfo = null;
           String networkUploadSpeedInfo = null;
           String networkDownloadTotalInfo = null;
           String networkUploadTotalInfo = null;
           if (showNetwork && networkIF != null) {
               networkIF.updateAttributes();
               long downloadBytes = networkIF.getBytesRecv();
               long uploadBytes = networkIF.getBytesSent();
               double downloadSpeedMbps = (double) (downloadBytes - lastDownloadBytes) * 8 / 1024 / 1024;
               double uploadSpeedMbps = (double) (uploadBytes - lastUploadBytes) * 8 / 1024 / 1024;
               if (downloadSpeedMbps < 0) downloadSpeedMbps = 0;
               if (uploadSpeedMbps < 0) uploadSpeedMbps = 0;
               lastDownloadBytes = downloadBytes;
               lastUploadBytes = uploadBytes;
               networkIPInfo = " ".repeat(15 - networkIF.getIPv4addr()[0].length()) + networkIF.getIPv4addr()[0];

               if (downloadSpeedMbps < 1) {
                   downloadSpeedMbps *= 1024;
                   networkDownloadSpeedInfo = String.format(" Download Speed:  %6.2f Kbps", downloadSpeedMbps);
               } else {
                   networkDownloadSpeedInfo = String.format(" Download Speed:  %6.2f Mbps", downloadSpeedMbps);
               }
               if (uploadSpeedMbps < 1) {
                   uploadSpeedMbps *= 1024;
                   networkUploadSpeedInfo = String.format(" Upload Speed:  %8.2f Kbps", uploadSpeedMbps);
               } else {
                   networkUploadSpeedInfo = String.format(" Upload Speed:  %8.2f Mbps", uploadSpeedMbps);
               }
               networkDownloadTotalInfo = String.format(" Download Total:  %6.2f  GiB", downloadBytes / 1e9);
               networkUploadTotalInfo = String.format(" Upload Total:  %8.2f  GiB", uploadBytes / 1e9);
           }

           String finalCpuUsageInfo = cpuUsageInfo;
           String finalCpuTemperatureInfo = cpuTemperatureInfo;
           String finalCpuNameInfo = cpuNameInfo;
           String finalGpuTemperatureInfo = gpuTemperatureInfo;
           String finalGpuUsageInfo = gpuUsageInfo;
           String finalGpuNameInfo = gpuNameInfo;
           String finalRamTotalInfo = ramTotalInfo;
           String finalRamInUseInfo = ramInUseInfo;
           String finalRamFreeInfo = ramFreeInfo;
           String finalSsdTotalInfo = ssdTotalInfo;
           String finalSsdFreeInfo = ssdFreeInfo;
           String finalSsdUsedInfo = ssdUsedInfo;
           String finalNetworkIPInfo = networkIPInfo;
           String finalNetworkDownloadSpeedInfo = networkDownloadSpeedInfo;
           String finalNetworkUploadSpeedInfo = networkUploadSpeedInfo;
           String finalNetworkDownloadTotalInfo = networkDownloadTotalInfo;
           String finalNetworkUploadTotalInfo = networkUploadTotalInfo;

           SwingUtilities.invokeLater(() -> {
               kernelLabel.setText(kernelInfo);
               uptimeLabel.setText(uptimeInfo);

               if (showCpu) {
                   cpuUsageLabel.setText(finalCpuUsageInfo);
                   cpuTemperatureLabel.setText(finalCpuTemperatureInfo);
                   cpuNameLabel.setText(finalCpuNameInfo);
               }

               if (showGpu) {
                   gpuTemperatureLabel.setText(finalGpuTemperatureInfo);
                   gpuUsageLabel.setText(finalGpuUsageInfo);
                   gpuNameLabel.setText(finalGpuNameInfo);
               }

               if (showRam) {
                   ramTotalLabel.setText(finalRamTotalInfo);
                   ramInUseLabel.setText(finalRamInUseInfo);
                   ramFreeLabel.setText(finalRamFreeInfo);
               }

               if (showSsd) {
                   ssdTotalLabel.setText(finalSsdTotalInfo);
                   ssdFreeLabel.setText(finalSsdFreeInfo);
                   ssdUsedLabel.setText(finalSsdUsedInfo);
               }

               if (showNetwork) {
                   networkIPLabel.setText(" IP:          " + finalNetworkIPInfo);
                   networkDownloadSpeedLabel.setText(finalNetworkDownloadSpeedInfo);
                   networkUploadSpeedLabel.setText(finalNetworkUploadSpeedInfo);
                   networkDownloadTotalLabel.setText(finalNetworkDownloadTotalInfo);
                   networkUploadTotalLabel.setText(finalNetworkUploadTotalInfo);
                   downloadMonitor.startDownload();
                   uploadMonitor.startUpload();
                   NetworkMonitor.setNetworkIF(networkIF);
                   if (networkIF.getIfOperStatus() == NetworkIF.IfOperStatus.DOWN) {
                       downloadMonitor.resetChart();
                       uploadMonitor.resetChart();
                   }
               }

               if (showProcess) {
                   processLabel.setText(String.format(" %-9s %7s %10s", "Name", "CPU (%)", "Memory"));
               }

               if (SettingsPanel.checkSettings) {
                   updateSetting();
                   SettingsPanel.checkSettings = Boolean.FALSE;
               }

               repaint();
           });
       } catch (Exception e) {
           System.out.println("Error updating system info: " + e.getMessage());
       }
   }


    public void initializeUI() {
        setMaximumSize(new Dimension(1000, getMaximumSize().height));
        int w = settings.getJSONObject("Screen").getInt("width");
        int h = settings.getJSONObject("Screen").getInt("height");
        setSize(new Dimension(w, h));
        int xc = settings.getJSONObject("Screen").getInt("xc");
        int yc = settings.getJSONObject("Screen").getInt("yc");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - getWidth() + xc;
        setLocation(x, yc);

        setResizable(false);
        setUndecorated(true);
        setAlwaysOnTop(false);
        if (settings.getJSONObject("Screen").getBoolean("alwaysOnTop")) {
            setAlwaysOnTop(true);
        }


        setFocusableWindowState(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        setBackgroundColorUpdate();
        setOpacityUpdate();

        timeLabel = createLabel("HH:mm");
        timeLabel.setBorder(new EmptyBorder(0, 12,0 , 0));
        timeLabel.setName("timeLabel");
        dateLabel = createLabel(" EEE, dd MMM yyyy");
        dateLabel.setName("dateLabel");
        weatherLabel = createLabel("Weather: --");
        BufferedImage blankImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = blankImage.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
        g2d.fillRect(0, 0, 50, 50);
        g2d.dispose();
        ImageIcon transparentIcon = new ImageIcon(blankImage);
        weatherLabel.setIcon(transparentIcon);
        kernelLabel = createLabel(" Computer:-- ");
        uptimeLabel = createLabel(" Uptime: 0h 0m 0s");
        cpuUsageLabel = createLabel(" CPU Usage: 0%");
        cpuTemperatureLabel = createLabel(" CPU Temperature: 0°C");
        cpuNameLabel = createLabel(" CPU: --");
        processLabel = createLabel(" Process: --");
        processListLabel[0] = createLabel("  --");
        processListLabel[1] = createLabel("  --");
        processListLabel[2] = createLabel("  --");
        processListLabel[3] = createLabel("  --");


        gpuNameLabel = createLabel(" GPU: --");
        gpuTemperatureLabel = createLabel(" GPU Temperature: 0°C");
        gpuUsageLabel = createLabel(" GPU Usage: 0%");

        ramTotalLabel = createLabel(" RAM Total: 0 GiB");
        ramInUseLabel = createLabel(" In Use: 0 GiB");
        ramFreeLabel = createLabel(" Free: 0 GiB");


        ssdTotalLabel = createLabel(" SSD Total: 0 GiB");
        ssdFreeLabel = createLabel(" Free: 0 GiB");
        ssdUsedLabel = createLabel(" Used: 0 GiB");

        networkIPLabel = createLabel(" IP: --");
        networkDownloadSpeedLabel = createLabel(" Download Speed: 0 kB/s");
        networkUploadSpeedLabel = createLabel(" Upload Speed: 0 kB/s");
        networkDownloadTotalLabel = createLabel(" Download Total: 0 MiB");
        networkUploadTotalLabel = createLabel(" Upload Total: 0 KiB");
        SYSTEMLabel = createSeparator("SYSTEM");
        CPULabel = createSeparator("CPU");
        GPULabel = createSeparator("GPU");
        RAMLabel = createSeparator("RAM");
        SSDLabel = createSeparator("STORAGE");
        NETWORKLabel = createSeparator("NETWORK");
        PROCESSESLabel = createSeparator("PROCESSES");


        if (settings.getJSONObject("Show/Hide").getJSONObject("DATETIME").getBoolean("showTime")) {
            panel.add(timeLabel);
        }
        if (settings.getJSONObject("Show/Hide").getJSONObject("DATETIME").getBoolean("showDate")) {
            panel.add(dateLabel);
        }

        if (showWeather) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("WEATHER").getBoolean("showWeather")) {
                panel.add(weatherLabel);
            }
        }
        if (settings.getJSONObject("Show/Hide").getJSONObject("SYSTEM").getBoolean("showSYSTEMTitle")) {
            panel.add(SYSTEMLabel);
        }
        if (settings.getJSONObject("Show/Hide").getJSONObject("KERNEL").getBoolean("showKernel")) {
            panel.add(kernelLabel);
        }
        if (settings.getJSONObject("Show/Hide").getJSONObject("SYSTEM").getBoolean("showUptime")) {
            panel.add(uptimeLabel);
        }
        if (showCpu) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("CPU").getBoolean("showCPUTitle")) {
                panel.add(CPULabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("CPU").getBoolean("showCpuName")) {
                panel.add(cpuNameLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("CPU").getBoolean("showCpuUsage")) {
                panel.add(cpuUsageLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("CPU").getBoolean("showCpuTemp")) {
                panel.add(cpuTemperatureLabel);
            }
        }
        if (showProcess) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("PROCESS").getBoolean("showProcessTitle")) {
                panel.add(PROCESSESLabel);

            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("PROCESS").getBoolean("showProcess")) {
                panel.add(processLabel);
            }
            panel.add(processListLabel[0]);
            panel.add(processListLabel[1]);
            panel.add(processListLabel[2]);
            panel.add(processListLabel[3]);
        }


        if (showGpu) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("GPU").getBoolean("showGPUTitle")) {
                panel.add(GPULabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("GPU").getBoolean("showGpuName")) {
                panel.add(gpuNameLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("GPU").getBoolean("showGpuUsage")) {
                panel.add(gpuUsageLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("GPU").getBoolean("showGpuTemp")) {
                panel.add(gpuTemperatureLabel);
            }
        }
        if (showRam) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("RAM").getBoolean("showRAMTitle")) {
                panel.add(RAMLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("RAM").getBoolean("showRamTotal")) {
                panel.add(ramTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("RAM").getBoolean("showRamInUse")) {
                panel.add(ramInUseLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("RAM").getBoolean("showRamFree")) {
                panel.add(ramFreeLabel);
            }
        }
        if (showSsd) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("STORAGE").getBoolean("showSSDTitle")) {
                panel.add(SSDLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("STORAGE").getBoolean("showSsdTotal")) {
                panel.add(ssdTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("STORAGE").getBoolean("showSsdFree")) {
                panel.add(ssdFreeLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("STORAGE").getBoolean("showSsdUsed")) {
                panel.add(ssdUsedLabel);
            }
        }

        downloadMonitor = new NetworkMonitor(networkIF, true);
        uploadMonitor = new NetworkMonitor(networkIF, false);

        if (showNetwork) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNETWORKTitle")) {
                panel.add(NETWORKLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNetworkIP")) {
                panel.add(networkIPLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNetworkDownloadSpeed")) {
                panel.add(networkDownloadSpeedLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNetworkDownloadTotal")) {
                panel.add(networkDownloadTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNetworkDownloadMonitor")) {
                panel.add(downloadMonitor);

            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNetworkUploadSpeed")) {
                panel.add(networkUploadSpeedLabel);
            }

            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNetworkUploadTotal")) {
                panel.add(networkUploadTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("NETWORK").getBoolean("showNetworkUploadMonitor")) {
                panel.add(uploadMonitor);

            }
        }
        add(panel);
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
            // settingsUI.setVisible(true);
           // if (settingsUI.isSettingsAccepted()) {
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

                WinDef.HWND hwnd = getHWnd(ui);
                int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
                exStyle |= WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
                User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);
           // }
        });
        new Thread(SystemTrayApp::new).start();
    }

}
