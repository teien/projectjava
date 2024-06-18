import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.*;
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
    private static JLabel PROCESSESLabel;
    static final JLabel[] processListLabel = new JLabel[5];
    static {
        for (int i = 0; i < 5; i++) {
            processListLabel[i] = new JLabel();
        }
    }
    private static String fontType1;
    private HardwareAbstractionLayer hal;
    private static OperatingSystem os;
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

    private static final Map<Integer, Long> previousTimes = new HashMap<>();
    private static final Map<Integer, Long> previousUpTimes = new HashMap<>();
    private final ScheduledExecutorService systemInfoExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService weatherUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService checkUpdateNet = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService checkUpdateWeather = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService processExecutor = Executors.newSingleThreadScheduledExecutor();
    private static int fontColor1;
    private static int fontColor2;
    private static String fontType2;
    private static int fontSize2;
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
    private int cpuNumber;
    private NetworkMonitor downloadMonitor;
    private NetworkMonitor uploadMonitor ;


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
            checkUpdateWeather.shutdown();
            processExecutor.shutdown();
        }));



    }
    public void initializeUI() {
        // Load settings from file
        JSONObject settings = SettingsLogger.loadSettings();
        setTitle("System Monitor");
        setMaximumSize(new Dimension(1000, getMaximumSize().height));
        int w = settings.getJSONObject("Screen").getInt("width");
        int h = settings.getJSONObject("Screen").getInt("height");
        setSize(new Dimension(w, h));
        int xc = settings.getJSONObject("Screen").getInt("xc");
        int yc = settings.getJSONObject("Screen").getInt("yc");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width -getWidth() + xc;
        setLocation(x, yc);

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

        timeLabel = createLabel("HH:mm");
        timeLabel.setName("timeLabel");
        dateLabel = createLabel("EEE, dd MMM yyyy");
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
        processListLabel[0] = createSmLabel();
        processListLabel[1] = createSmLabel();
        processListLabel[2] = createSmLabel();
        processListLabel[3] = createSmLabel();


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

        panel.add(timeLabel);
        panel.add(dateLabel);
        if (showWeather) {
            if (settings.getJSONObject("Show/Hide").getBoolean("showWeather")) {
                panel.add(weatherLabel);
            }
        }
        if(settings.getJSONObject("Show/Hide").getBoolean("showSYSTEMTitle")){
            panel.add(SYSTEMLabel);
        }
        if(settings.getJSONObject("Show/Hide").getBoolean("showKernel")){
            panel.add(kernelLabel);
        }
        if(settings.getJSONObject("Show/Hide").getBoolean("showUptime")){
            panel.add(uptimeLabel);
        }
        if (showCpu) {
            if (settings.getJSONObject("Show/Hide").getBoolean("showCPUTitle")) {
                panel.add(CPULabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showCpuName")) {
                panel.add(cpuNameLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showCpuUsage")) {
                panel.add(cpuUsageLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showCpuTemp")) {
                panel.add(cpuTemperatureLabel);
            }
        }
        if (showProcess) {
           if (settings.getJSONObject("Show/Hide").getBoolean("showProcessTitle")) {
               panel.add(PROCESSESLabel);

           }
           if (settings.getJSONObject("Show/Hide").getBoolean("showProcess")) {
               panel.add(processLabel);
           }
           panel.add(processListLabel[0]);
           panel.add(processListLabel[1]);
           panel.add(processListLabel[2]);
           panel.add(processListLabel[3]);
        }



        if (showGpu) {
            if (settings.getJSONObject("Show/Hide").getBoolean("showGPUTitle")) {
                panel.add(GPULabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showGpuName")) {
                panel.add(gpuNameLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showGpuUsage")) {
                panel.add(gpuUsageLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showGpuTemp")) {
                panel.add(gpuTemperatureLabel);
            }


        }
        if (showRam) {
            if (settings.getJSONObject("Show/Hide").getBoolean("showRAMTitle")) {
                panel.add(RAMLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showRamTotal")) {
                panel.add(ramTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showRamInUse")) {
                panel.add(ramInUseLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showRamFree")) {
                panel.add(ramFreeLabel);
            }
        }
        if (showSsd) {
            if (settings.getJSONObject("Show/Hide").getBoolean("showSSDTitle")) {
                panel.add(SSDLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showSsdTotal")) {
                panel.add(ssdTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showSsdFree")) {
                panel.add(ssdFreeLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showSsdUsed")) {
                panel.add(ssdUsedLabel);
            }
        }
         downloadMonitor = new NetworkMonitor(networkIF, true);
        downloadMonitor.setBorder(new EmptyBorder(0, -10, 0, 4));
         uploadMonitor = new NetworkMonitor(networkIF, false);
        uploadMonitor.setBorder(new EmptyBorder(0, -10, 0, 4));
        if (showNetwork) {
            if (settings.getJSONObject("Show/Hide").getBoolean("showNETWORKTitle")) {
                panel.add(NETWORKLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showNetworkIP")) {
                panel.add(networkIPLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showNetworkDownloadSpeed")) {
                panel.add(networkDownloadSpeedLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showNetworkDownloadTotal")) {
                panel.add(networkDownloadTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showNetworkDownloadMonitor")) {
                panel.add(downloadMonitor);
            }

            if (settings.getJSONObject("Show/Hide").getBoolean("showNetworkUploadSpeed")) {
                panel.add(networkUploadSpeedLabel);
            }

            if (settings.getJSONObject("Show/Hide").getBoolean("showNetworkUploadTotal")) {
                panel.add(networkUploadTotalLabel);
            }
            if (settings.getJSONObject("Show/Hide").getBoolean("showNetworkUploadMonitor")) {
                panel.add(uploadMonitor);

            }


        }
        getContentPane().add(panel);


    }

    private void setBackgroundColorUpdate() {
        JSONObject settings = SettingsLogger.loadSettings();
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
    private void initializeSystemInfo() {
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
                    this.networkIF = networkIF;
                    break;
                }
            }
        }
    }

    private void startUpdateTimer() {
        // Update weather if first time can not be done because of network issues
        if ((showNetwork) && (networkIPLabel.getText().equals(" IP: --"))){
            checkUpdateNet.scheduleAtFixedRate(() -> {
                SwingUtilities.invokeLater(this::initializeSystemInfo);
                if (!networkIPLabel.getText().equals(" IP: --")) {
                    checkUpdateNet.shutdown();
                }
            }, 20, 10, TimeUnit.SECONDS);
        }
       // Update weather if first time can not be done because of network issues
        if (((showWeather) && (!updateWeatherInfo())) || (weatherLabel.getText().equals("Weather: --"))) {
            System.out.println(weatherLabel.getText());

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
        systemInfoExecutor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::updateSystemInfo ), 0, 1, TimeUnit.SECONDS);
        ProcessMonitor pm = new  ProcessMonitor( os,  previousTimes,  previousUpTimes,  cpuNumber, processListLabel);
        processExecutor.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(() -> {
            if (showProcess) {
                pm.printProcesses();
            }
        }), 0, 3/2, TimeUnit.SECONDS);

    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        JSONObject settings = SettingsLogger.loadSettings();
        fontType1 = settings.getJSONObject("Style").getString("fontType1");
        int fontSize1 = settings.getJSONObject("Style").getInt("fontSize1");
        fontColor1 = settings.getJSONObject("Style").optInt("fontColor1", Color.WHITE.getRGB());
        if (Objects.equals(label.getText(), "HH:mm") || Objects.equals(label.getText(), "EEE, dd MMM yyyy") ){
            label.setFont(new Font(fontType1, Font.PLAIN, 21));
        }else {
            label.setFont(new Font(fontType1, Font.PLAIN, fontSize1));
        }
        label.setForeground(new Color(fontColor1));
        return label;
    }
    private  JLabel createSmLabel() {
        JLabel label = new JLabel("    --");
        JSONObject settings = SettingsLogger.loadSettings();
        fontType1 = settings.getJSONObject("Style").getString("fontType1");
        int fontSize1 = settings.getJSONObject("Style").getInt("fontSize1") - 1;
        fontColor1 = settings.getJSONObject("Style").optInt("fontColor1", Color.WHITE.getRGB());
        label.setFont(new Font(fontType1, Font.PLAIN, fontSize1));
        label.setForeground(new Color(fontColor1));
        return label;
    }
    void updateSetting(){
        setFontUpdate(processLabel, timeLabel,dateLabel,weatherLabel,kernelLabel,uptimeLabel,cpuUsageLabel,cpuTemperatureLabel,cpuNameLabel,ramTotalLabel,ramInUseLabel,ramFreeLabel,ssdTotalLabel,ssdFreeLabel,ssdUsedLabel,networkIPLabel,networkDownloadSpeedLabel,networkUploadSpeedLabel,networkDownloadTotalLabel,networkUploadTotalLabel,gpuTemperatureLabel,gpuUsageLabel,gpuNameLabel);
        setFontUpdateTitle(PROCESSESLabel,SYSTEMLabel,CPULabel,GPULabel,RAMLabel,SSDLabel,NETWORKLabel);
        setOpacityUpdate();
        setBackgroundColorUpdate();
        setFontSmUpdate(processListLabel[0],processListLabel[1],processListLabel[2],processListLabel[3]);
        updateLocationScreen();
    }
    void updateLocationScreen(){
        JSONObject settings = SettingsLogger.loadSettings();
        int w = settings.getJSONObject("Screen").getInt("width");
        int h = settings.getJSONObject("Screen").getInt("height");
        int xc = settings.getJSONObject("Screen").getInt("xc");
        int yc = settings.getJSONObject("Screen").getInt("yc");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width -w + xc;
        setLocation(x, yc);
        setSize(new Dimension(w, h));
    }
    void setFontSmUpdate(JLabel... labels){
        JSONObject settings = SettingsLogger.loadSettings();
        int sm = settings.getJSONObject("Style").getInt("fontSize1") -1;
        for (JLabel label : labels) {
            label.setFont(new Font(fontType1, Font.PLAIN, sm));
            label.setForeground(new Color(fontColor1));
        }
    }
    void setOpacityUpdate(){
        JSONObject settings = SettingsLogger.loadSettings();
        if (!settings.isEmpty()) {
            double opacity = settings.getJSONObject("Style").getDouble("opacity");
            this.setOpacity((float) opacity);
        }
    }
    static void setFontUpdate(JLabel... labels){
        JSONObject settings = SettingsLogger.loadSettings();
            fontType1 = settings.getJSONObject("Style").getString("fontType1");
        int fontSize1 = settings.getJSONObject("Style").getInt("fontSize1");
            fontColor1 = settings.getJSONObject("Style").optInt("fontColor1", Color.WHITE.getRGB());
        for (JLabel label : labels) {
            label.setFont(new Font(fontType1, Font.PLAIN, fontSize1));
            label.setForeground(new Color(fontColor1));
            if (Objects.equals(label.getName(),"timeLabel") || Objects.equals(label.getName(),"dateLabel")){
                label.setFont(new Font(fontType1, Font.PLAIN, 21));
                label.setForeground(new Color(fontColor1));
            }
        }
    }
    static void setFontUpdateTitle(JLabel... labels){
        JSONObject settings = SettingsLogger.loadSettings();
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
        JSONObject settings = SettingsLogger.loadSettings();
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
            timeLabel.setText(timeFormat.format(now));
            dateLabel.setText(dateFormat.format(now));

            String computerName = System.getenv("COMPUTERNAME");
            String osName = System.getProperty("os.name").toLowerCase();
            kernelLabel.setText(" Computer: " + computerName + " (" + osName + ")");
            uptimeLabel.setText(" Uptime: " + formatUptime(os.getSystemUptime()));

            ServiceManager.HwInfo hwInfo = ServiceManager.HwInfo.getHwInfo();
            if (showCpu) {
                Double cpuLoad = hwInfo.cpuUsage();
                cpuUsageLabel.setText(String.format(" CPU Usage: %15.1f%%", cpuLoad));
                Double cpuTemperature = hwInfo.cpuTemperature();
                cpuTemperatureLabel.setText(String.format(" CPU Temperature: %9.1f°C", cpuTemperature));
                cpuNameLabel.setText(" "+hwInfo.cpuName()) ;

            }
            if (showGpu) {
                Double gpuTemperature = hwInfo.gpuTemperature();
                Double gpuUsage = hwInfo.gpuUsage();
                gpuTemperatureLabel.setText(" GPU Temperature: " + String.format("%9.1f°C", gpuTemperature));
                gpuUsageLabel.setText(String.format(" GPU Usage: %15.1f%%", gpuUsage));
                gpuNameLabel.setText(" "+hwInfo.gpuName());
            }

            GlobalMemory memory = hal.getMemory();
            if (showRam) {
                long totalMemory = memory.getTotal();
                long availableMemory = memory.getAvailable();
                long usedMemory = totalMemory - availableMemory;
                ramTotalLabel.setText(String.format(" RAM Total:  %12.2f GiB", totalMemory / 1e9));
                ramInUseLabel.setText(String.format(" In Use:  %6.1f%%", (double) usedMemory / totalMemory * 100) +   String.format("%8.2f GiB", usedMemory / 1e9) );
                ramFreeLabel.setText(String.format(" Free:  %8.1f%%", (double) availableMemory / totalMemory * 100) +  String.format("%8.2f GiB", availableMemory / 1e9) );

            }

            if (showSsd && !hal.getDiskStores().isEmpty()) {
                File diskPartition = new File("C:");
                long totalDisk = diskPartition.getTotalSpace();
                long usableDisk = diskPartition.getFreeSpace();
                long usedDisk = totalDisk - usableDisk;
                ssdTotalLabel.setText(String.format(" SSD Total:  %12.2f GiB", totalDisk / 1e9));
                ssdFreeLabel.setText(String.format(" Free:  %17.2f GiB", usableDisk / 1e9));
                ssdUsedLabel.setText(String.format(" Used:  %17.2f GiB", usedDisk / 1e9));
            }

            if (showNetwork){
            if (networkIF != null) {
               networkIF.updateAttributes();
                long downloadBytes = networkIF.getBytesRecv();
                long uploadBytes = networkIF.getBytesSent();
                double downloadSpeedMbps = (double) (downloadBytes - lastDownloadBytes) * 8 / 1024 / 1024;
                double uploadSpeedMbps = (double) (uploadBytes - lastUploadBytes) * 8 / 1024 / 1024;
                lastDownloadBytes = downloadBytes;
                lastUploadBytes = uploadBytes;
                networkIPLabel.setText(" IP:             " + networkIF.getIPv4addr()[0]);

                if (downloadSpeedMbps < 1){
                    downloadSpeedMbps = downloadSpeedMbps * 1024;
                    networkDownloadSpeedLabel.setText(String.format(" Download Speed:  %6.2f Kbps", downloadSpeedMbps));
                } else
                {
                    networkDownloadSpeedLabel.setText(String.format(" Download Speed:  %6.2f Mbps", downloadSpeedMbps));
                }
                if (uploadSpeedMbps < 1){
                    uploadSpeedMbps = uploadSpeedMbps * 1024;
                    networkUploadSpeedLabel.setText(String.format(" Upload Speed:  %8.2f Kbps", uploadSpeedMbps));
                } else {
                    networkUploadSpeedLabel.setText(String.format(" Upload Speed:  %8.2f Mbps", uploadSpeedMbps));
                }
                networkDownloadTotalLabel.setText(String.format(" Download Total:  %7.2f GiB", downloadBytes / 1e9));
                networkUploadTotalLabel.setText(String.format(" Upload Total:  %9.2f GiB", uploadBytes / 1e9));
                downloadMonitor.startDownload();
                uploadMonitor.startUpload();
                }
            }
            if (showProcess) {
                String htmlText = "<html>"
                        + "<style>"
                        + "table { border-spacing: 0; }"
                        + "td { padding: 0; }"
                        + "</style>"
                        + "<table>"
                        + "<tr>"
                        + "<td style='padding-right: 27px;'>&nbsp;Name</td>"
                        + "<td style='padding-right: 20px;'>CPU (%)</td>"
                        + "<td>Memory</td>"
                        + "</tr>"
                        + "</table>"
                        + "</html>";
                processLabel.setText(htmlText);



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
                        ex.printStackTrace();
                        return false;
                    });
            boolean result = future.join();
            System.out.println("Weather update " + (result ? "success" : "failure"));
            return result;
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

    public static void main(String[] args)  {
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
                        settingsUI.isWeatherSelected(),
                        settingsUI.isGpuSelected(),
                        settingsUI.isProcessSelected()
                );
                ui.setVisible(true);
                new SystemTrayApp();
                WinDef.HWND hwnd = getHWnd(ui);
                int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
                exStyle |= WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
                User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);


            }
        });
    }

}
