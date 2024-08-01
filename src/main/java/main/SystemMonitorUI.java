package main;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.json.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;
import settings.SettingUI;
import settings.SettingsLogger;
import settings.SettingsPanel;
import system.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;

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
    static final JLabel[] processListLabel = new JLabel[4];

    private static String fontType1;
    private static HardwareAbstractionLayer hal;
    private static OperatingSystem os;
    private static NetworkIF networkIF;
    private static File[] drives;
    private static String cpuName;
    private  double cpuLoad;
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
    private final ScheduledExecutorService processExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService cpuLoadExecutor =  Executors.newSingleThreadScheduledExecutor()    ;

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
    private static final JLabel[] diskLabel = new JLabel[30];

    private final JProgressBar[] diskProgressBars = new JProgressBar[30];
    private final JPanel[] progressBarPanel = new JPanel[30];

    private static CentralProcessor processor;
    private static long[] ticks;
    private static String gpuName;
    private JProgressBar ramProgressBars;

    public SystemMonitorUI(boolean showCpu, boolean showRam, boolean showSsd, boolean showNetwork, boolean showWeather, boolean showGpu, boolean showProcess) {
        this.showCpu = showCpu;
        this.showRam = showRam;
        this.showSsd = showSsd;
        this.showNetwork = showNetwork;
        this.showWeather = showWeather;
        this.showGpu = showGpu;
        this.showProcess = showProcess;



        initializeSystemInfo();
        updateDiskInfo();
        initializeUI();
        startUpdateTimer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            systemInfoExecutor.shutdown();
            weatherUpdateExecutor.shutdown();
            checkUpdateNet.shutdown();
            processExecutor.shutdown();
            cpuLoadExecutor.shutdown();
        }));

    }

    public static void initializeSystemInfo() {
        settings = SettingsLogger.loadSettings();
        SystemInfo systemInfo = new SystemInfo();
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);
        processor = systemInfo.getHardware().getProcessor();
        ticks = processor.getSystemCpuLoadTicks();
        cpuNumber = processor.getLogicalProcessorCount();
        cpuName = processor.getProcessorIdentifier().getName();
        String loadSettingCpuName = settings.getJSONObject("Show/Hide").getJSONObject("CPU").getString("cpuName");
        if(!loadSettingCpuName.isEmpty()){
            SystemMonitorUI.cpuName = loadSettingCpuName;
        }
        hal = systemInfo.getHardware();
        os = systemInfo.getOperatingSystem();
        //gpu
        for (GraphicsCard gpu : hal.getGraphicsCards()) {
            String gpuName = gpu.getName();
            boolean isDedicated = isDedicatedGPU(gpuName);
            if (isDedicated && settings.getJSONObject("Show/Hide").getJSONObject("GPU").getBoolean("showDedicatedGPU")) {
                SystemMonitorUI.gpuName = gpuName;
                break;
            } else if (!isDedicated && settings.getJSONObject("Show/Hide").getJSONObject("GPU").getBoolean("showIntegratedGPU")) {
                SystemMonitorUI.gpuName = gpuName;
                break;
            }
        }
        String loadSettingGpuName = settings.getJSONObject("Show/Hide").getJSONObject("GPU").getString("gpuName");
        if(!loadSettingGpuName.isEmpty()){
            SystemMonitorUI.gpuName = loadSettingGpuName;
        }
        //network
        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        if (!networkIFs.isEmpty()) {
            for (NetworkIF networkIF : networkIFs) {
                if (networkIF.isConnectorPresent() && networkIF.getIPv4addr().length > 0 && networkIF.getSpeed() > 0 && networkIF.getIfOperStatus().equals(NetworkIF.IfOperStatus.UP)) {
                    SystemMonitorUI.networkIF = networkIF;
                    break;
                }
            }
        }
        //disk
        drives = File.listRoots();
        boolean showAllDisk = settings.getJSONObject("Disk").getBoolean("showAllDisk");
        if (showAllDisk) {
            drives = Arrays.stream(drives).filter(drive -> drive.getTotalSpace() > 0).toArray(File[]::new);
        } else {
            String drivesList = settings.getJSONObject("Disk").getString("diskName");
            String[] driveList = drivesList.split(",");
            List<File> driveListFiltered = new ArrayList<>();
            for (String driveName : driveList) {
                for (File drive : drives) {
                    if (drive.toString().contains(driveName)) {
                        driveListFiltered.add(drive);
                    }
                }
            }
            drives = driveListFiltered.toArray(new File[0]);
        }


    }
    private static boolean isDedicatedGPU(String gpuName) {
        String lowerCaseName = gpuName.toLowerCase();
        if (lowerCaseName.contains("nvidia") ||
                (lowerCaseName.contains("amd") && !lowerCaseName.contains("radeon vega"))) {
            return true;
        } else if (lowerCaseName.contains("intel") ||
                (lowerCaseName.contains("amd") && lowerCaseName.contains("radeon vega"))) {
            return false;
        } else {

            return false;
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
        ticks = processor.getSystemCpuLoadTicks();
        cpuLoadExecutor.scheduleAtFixedRate(this::getCpuLoad, 0, 1, TimeUnit.SECONDS);
        if (showWeather) {
            weatherUpdateExecutor.scheduleAtFixedRate(SystemMonitorUI::updateWeatherInfo, 0, 30, TimeUnit.MINUTES);
        }
        systemInfoExecutor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(() -> {
            initializeSystemInfo();
            updateSystemInfo();
            updateLocationScreen();

        }), 0, 1, TimeUnit.SECONDS);

        if (showProcess) {
            ProcessMonitor pm = new ProcessMonitor(os, processListLabel);
            Predicate<OSProcess> filter = process -> {
                String name = process.getName();
                int pid = process.getProcessID();
                return !name.equals("Idle") && pid != 0 && !name.equals("cmd") &&
                        !name.equals("System") && !name.equals("svchost") &&
                        !name.equals("conhost") && !name.equals("explorer") &&
                        !name.isEmpty();
            };

            processExecutor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(() -> {
                pm.printProcesses(filter);
                repaint();
            }), 0, 1, TimeUnit.SECONDS);
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
    public static void updateDiskInfo() {
        String diskInfo;
        AtomicReferenceArray<String> driveInfos;
            File[] drives = File.listRoots();
            if (drives != null && drives.length > 0) {
                driveInfos = new AtomicReferenceArray<>(new String[drives.length]);
                for (int i = 0; i < drives.length; i++) {
                    if (drives[i].getTotalSpace() >0){
                        File drive = drives[i];
                        String driveName = drive.toString().replace("\\", "");
                        long totalSpace = drive.getTotalSpace();
                        long usableSpace = drive.getUsableSpace();
                        if (totalSpace < 1e12) {
                            diskInfo = String.format(" %s %12.2f GB/%.2f GB",
                                    driveName,
                                    usableSpace / 1e9,
                                    totalSpace / 1e9);
                        } else {
                            diskInfo = String.format(" %s %12.2f TB/%.2f TB",
                                    driveName,
                                    usableSpace / 1e12,
                                    totalSpace / 1e12);
                        }
                        driveInfos.set(i, diskInfo);
                    }
                    else {
                        driveInfos.set(i, "");
                    }
                }

            }

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
        setFontUpdate(processListLabel[0], processListLabel[1], processListLabel[2], processListLabel[3],processLabel, timeLabel,dateLabel, weatherLabel, kernelLabel, uptimeLabel, cpuUsageLabel, cpuTemperatureLabel, cpuNameLabel, ramTotalLabel, ramInUseLabel, networkIPLabel, networkDownloadSpeedLabel, networkUploadSpeedLabel, networkDownloadTotalLabel, networkUploadTotalLabel, gpuTemperatureLabel, gpuUsageLabel, gpuNameLabel);
        for (int i = 0; i < drives.length; i++) {
            if (diskLabel[i] != null) {
                setFontUpdate(diskLabel[i]);
            }

        }
        setFontUpdateTitle(PROCESSESLabel, SYSTEMLabel, CPULabel, GPULabel, RAMLabel, SSDLabel, NETWORKLabel);
        setOpacityUpdate();
        setBackgroundColorUpdate();
        updateLocationScreen();
        initializeSystemInfo();
        this.setAlwaysOnTop(settings.getJSONObject("Screen").getBoolean("alwaysOnTop"));
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
           if (showCpu) {
               cpuUsageInfo = String.format(" CPU Usage: %15.1f %%", cpuLoad);
               Double cpuTemperature = hwInfo.cpuTemperature();
               cpuTemperatureInfo = String.format(" CPU Temperature: %9.1f°C", cpuTemperature);
           }

           String gpuTemperatureInfo = null;
           String gpuUsageInfo = null;

           if (showGpu) {
               if(settings.getJSONObject("Show/Hide").getJSONObject("GPU").getBoolean("showDedicatedGPU")){
                   gpuUsageInfo = String.format(" GPU Usage: %15.1f %%", hwInfo.gpuUsage());
                   gpuTemperatureInfo = String.format(" GPU Temperature: %9.1f°C", hwInfo.gpuTemperature());
               } else {
                   gpuUsageInfo = String.format(" iGPU Usage: %14.1f %%", hwInfo.iGpuUsage());
                   gpuTemperatureInfo = String.format(" iGPU Temperature: %8.1f°C", hwInfo.cpuTemperature());
               }

           }
           String ramTotalInfo = null;
           long totalMemory;
           long availableMemory = 0;
           long usedMemory;
           GlobalMemory memory = hal.getMemory();
           if (showRam) {
               totalMemory = memory.getTotal();
               availableMemory = memory.getAvailable();
               usedMemory = totalMemory - availableMemory;
               ramTotalInfo = String.format(" RAM:       %-5.2f GiB/%3.0f GiB",usedMemory/1e9, totalMemory / 1e9);
           } else {
               totalMemory = 0;
               usedMemory = 0;
           }

           String diskInfo = null;
           String[] driveInfos = new String[30];
           long[] driveTotalSpace = new long[30];
           long[] driveUsageSpace = new long[30];
           if (showSsd) {
               if (drives.length > 0) {
                   for (int i = 0; i < drives.length; i++) {
                       driveTotalSpace[i] = 0;
                       driveUsageSpace[i] = 0;
                       if (drives[i].getTotalSpace() > 0) {
                           File drive = drives[i];
                           String driveName = drive.toString().replace("\\", "");
                           long totalSpace = drive.getTotalSpace();
                           long usableSpace = drive.getUsableSpace();
                           long usedSpace = totalSpace - usableSpace;
                           if (totalSpace < 1e12) {
                               diskInfo = String.format(" %s        %-6.2f GiB/%3.0f GiB",
                                       driveName,
                                       usedSpace/ 1e9,
                                       totalSpace / 1e9);
                           } else {
                               diskInfo = String.format(" %s            %-3.2f TB/%3.0f TB",
                                       driveName,
                                       usableSpace / 1e12,
                                       totalSpace / 1e12);
                           }
                           driveInfos[i] = diskInfo;
                           driveTotalSpace[i] = (long) (totalSpace / 1e9);
                           driveUsageSpace[i] = (long) (usedSpace / 1e9);
                       }    else {
                           driveInfos[i] = "";
                       }
                   }

               }
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

           String finalCpuName = cpuName;
           String finalCpuUsageInfo = cpuUsageInfo;
           String finalCpuTemperatureInfo = cpuTemperatureInfo;
           String finalGpuTemperatureInfo = gpuTemperatureInfo;
           String finalGpuUsageInfo = gpuUsageInfo;
           String finalGpuName = gpuName;
           String finalRamTotalInfo = ramTotalInfo;
           String finalNetworkIPInfo = networkIPInfo;
           String finalNetworkDownloadSpeedInfo = networkDownloadSpeedInfo;
           String finalNetworkUploadSpeedInfo = networkUploadSpeedInfo;
           String finalNetworkDownloadTotalInfo = networkDownloadTotalInfo;
           String finalNetworkUploadTotalInfo = networkUploadTotalInfo;

           String[] finalDiskInfo = new String[driveInfos.length];
           System.arraycopy(driveInfos, 0, finalDiskInfo, 0, 30);

           SwingUtilities.invokeLater(() -> {
               kernelLabel.setText(kernelInfo);
               uptimeLabel.setText(uptimeInfo);

               if (showCpu) {
                   cpuUsageLabel.setText(finalCpuUsageInfo);
                   cpuTemperatureLabel.setText(finalCpuTemperatureInfo);
                   cpuNameLabel.setText(" "+finalCpuName);

               }

               if (showGpu) {
                   gpuTemperatureLabel.setText(finalGpuTemperatureInfo);
                   gpuUsageLabel.setText(finalGpuUsageInfo);
                   gpuNameLabel.setText(" " + finalGpuName);
               }

               if (showRam) {
                   ramTotalLabel.setText(finalRamTotalInfo);
                   updateRamProgressBar( usedMemory,  totalMemory);
               }
               if (showSsd) {
                     for (int i = 0; i < drives.length+20; i++) {
                         if (diskLabel[i] != null) {
                             diskLabel[i].setText(finalDiskInfo[i]);
                             if (driveTotalSpace[i] > 0) {
                                 updateDiskProgressBar(i, driveUsageSpace[i], driveTotalSpace[i]);
                                  progressBarPanel[i].setVisible(true);
                                 diskLabel[i].setVisible(true);
                             }  else {
                                 diskLabel[i].setText("");
                                 progressBarPanel[i].setVisible(false);
                                 diskLabel[i].setVisible(false);
                             }

                         }
                     }
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
               revalidate();
               repaint();
           });
       } catch (Exception e) {
           System.out.println("Error updating system info: " + e.getMessage());
       }
   }

    private void getCpuLoad()  {
        long[] prevTicks = ticks;
        cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks)*100;
        if (cpuLoad < 0) {
            cpuLoad = 0;
        }
        if (cpuLoad > 100) {
            cpuLoad = 100;
        }
        ticks = processor.getSystemCpuLoadTicks();
    }

    private void updateDiskProgressBar(int index, long usedSpace, long totalSpace) {
        int percent = (int) (usedSpace * 100 / totalSpace);

        SwingUtilities.invokeLater(() -> {
            diskProgressBars[index].setValue( percent);
            diskProgressBars[index].setString(percent + "%");
            repaint();
        });
    }
    private void updateRamProgressBar(long usedSpace, long totalSpace) {
        int percent = (int) (usedSpace * 100 / totalSpace);
        SwingUtilities.invokeLater(() -> {
            ramProgressBars.setValue(percent);
            ramProgressBars.setString(percent + "%");
            repaint();
        });
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
        setLayout(new BorderLayout());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalGlue());

        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        setBackgroundColorUpdate();
        setOpacityUpdate();

        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeLabel = createLabel("HH:mm");
        timeLabel.setBorder(new EmptyBorder(0, 16,0 , 0));
        timeLabel.setName("timeLabel");
        dateLabel = createLabel(" EEE, dd MMM yyyy");
        dateLabel.setBorder(new EmptyBorder(0, 12, 0, 0));
        dateLabel.setName("dateLabel");
        weatherLabel = createLabel("Weather: --");
        weatherLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
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
        ramProgressBars = createProgressBar();


        int progressBarHeight = settings.getJSONObject("ProgressBar").optInt("progressBarHeight", 12);
        for (int i = 0; i < drives.length; i++) {
            diskLabel[i] = createLabel(" --");
            diskProgressBars[i] = createProgressBar();
            progressBarPanel[i]  = createProgressBarPanel(diskProgressBars[i], progressBarHeight);
        }
        for (int i = drives.length; i < 20; i++) {
            diskLabel[i] = createLabel("");
            diskProgressBars[i] = createProgressBar();
            progressBarPanel[i]  = createProgressBarPanel(diskProgressBars[i], progressBarHeight);
            diskLabel[i].setVisible(false);
            progressBarPanel[i].setVisible(false);
        }


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
                panel.add(processListLabel[0]);
                panel.add(processListLabel[1]);
                panel.add(processListLabel[2]);
                panel.add(processListLabel[3]);
            }
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
            if (settings.getJSONObject("Show/Hide").getJSONObject("RAM").getBoolean("showRamProgressBar")) {
                JPanel ramProgressBarsPanel = new JPanel();
                ramProgressBarsPanel.setLayout(new GridLayout(0, 1));
                ramProgressBarsPanel.setPreferredSize(new Dimension(230, 10));
                ramProgressBarsPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                ramProgressBarsPanel.setOpaque(true);
                ramProgressBarsPanel.setBackground(new Color(0, 0, 0, 0));

                ramProgressBarsPanel.add(ramProgressBars);
                panel.add(ramProgressBarsPanel);
            }

        }
        if (showSsd) {
            if (settings.getJSONObject("Show/Hide").getJSONObject("STORAGE").getBoolean("showSSDTitle")) {
                panel.add(SSDLabel);
            }
            if (settings.getJSONObject("Show/Hide").getJSONObject("STORAGE").getBoolean("showDisk")) {
                for (int i = 0; i < drives.length; i++) {
                    panel.add(diskLabel[i]);
                    panel.add(progressBarPanel[i]);
                    diskLabel[i].setVisible(true);
                    progressBarPanel[i].setVisible(true);
                }
                for (int i = drives.length; i < 20; i++) {
                    panel.add(diskLabel[i]);
                    panel.add(progressBarPanel[i]);
                    diskLabel[i].setVisible(false);
                    progressBarPanel[i].setVisible(false);
                }
        }}

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
        panel.add(Box.createVerticalGlue());
        add(panel);

    }
    private JPanel createProgressBarPanel(JProgressBar progressBar, int height) {
        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new GridLayout(0, 1));
        progressBarPanel.setPreferredSize(new Dimension(230, height));
        progressBar.setPreferredSize(new Dimension(230, height));
        progressBarPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        progressBarPanel.setOpaque(true);
        progressBarPanel.setBackground(new Color(0, 0, 0, 0));
        progressBarPanel.add(progressBar);
        return progressBarPanel;
    }

    private JProgressBar createProgressBar() {
        JProgressBar progressBar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();

                // Vẽ nền thanh tiến trình
                g2d.setColor(new Color(settings.getJSONObject("ProgressBar").getInt("progressBarBackgroundColor")));
                g2d.fillRect(0, 0, width, height);

                // Vẽ thanh tiến trình
                int progressWidth = (int) (((double) getValue() / getMaximum()) * width);
                g2d.setColor(new Color(settings.getJSONObject("ProgressBar").getInt("progressBarForegroundColor")));
                g2d.fillRect(0, 0, progressWidth, height);

                // Vẽ bóng đổ nhẹ
                g2d.setColor(new Color(0, 0, 0, 50)); // Bóng đổ nhẹ
                g2d.fillRect(2, 2, progressWidth - 4, height - 4);

                // Vẽ phần trăm
                if (isStringPainted()) {
                    String progressText = getString();
                    FontMetrics fontMetrics = g2d.getFontMetrics();
                    int stringWidth = fontMetrics.stringWidth(progressText);
                    int stringHeight = fontMetrics.getAscent();

                    // Tính toán vị trí để vẽ văn bản
                    int x = (width - stringWidth) / 2;
                    int y = (height + stringHeight) / 2 - 2;

                    // Đặt hiệu ứng đổ bóng cho văn bản
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Vẽ viền văn bản
                    g2d.setColor(Color.BLACK); // Màu viền
                    for (int i = -1; i <= 1; i++) {
                        for (int j = -1; j <= 1; j++) {
                            if (i != 0 || j != 0) {
                                g2d.drawString(progressText, x + i, y + j);
                            }
                        }
                    }

                    // Vẽ văn bản chính
                    g2d.setColor(Color.WHITE); // Màu văn bản chính
                    g2d.drawString(progressText, x, y);
                }
            }

        };
        progressBar.setBorderPainted(false);
        progressBar.setOrientation(SwingConstants.HORIZONTAL);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("0%");
        progressBar.setFont(new Font("JetBrains Mono NL ExtraLight", Font.BOLD, 9));

        return progressBar;
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
