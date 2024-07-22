package settings;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

public class SettingsLogger {
    private static final String SETTINGS_FILE = "settings.json";
    private static final String DEFAULT_SETTINGS = """
            {
                     "Show/Hide": {
                         "STORAGE": {
                             "showDisk": true,
                             "showSSDTitle": true
                         },
                         "SYSTEM": {
                             "showUptime": true,
                             "showSYSTEMTitle": true
                         },
                         "NETWORK": {
                             "showNetworkIP": true,
                             "showNetworkUploadMonitor": true,
                             "showNetworkDownloadTotal": true,
                             "showNETWORKTitle": true,
                             "showNetworkDownloadSpeed": true,
                             "showNetworkUploadSpeed": true,
                             "showNetworkDownloadMonitor": true,
                             "showNetworkUploadTotal": true
                         },
                         "KERNEL": {"showKernel": true},
                         "WEATHER": {"showWeather": true},
                         "DATETIME": {
                             "showTime": true,
                             "showTimeTitle": true,
                             "showDate": true
                         },
                         "PROCESS": {
                             "showProcess": true,
                             "showProcessTitle": true
                         },
                         "CPU": {
                             "showCPUTitle": true,
                             "showCpuName": true,
                             "showCpuUsage": true,
                             "showCpuTemp": true,
                             "cpuName": ""
                 
                         },
                         "GPU": {
                             "showDedicatedGPU": true,
                             "showIntegratedGPU": false,
                             "showGpuName": true,
                             "showGpuUsage": true,
                             "showGpuTemp": true,
                             "showGPUTitle": true,
                             "gpuName": ""
                         },
                         "RAM": {
                             "showRamProgressBar": true,
                             "showRamTotal": true,
                             "showRAMTitle": true
                         }
                     },
                     "PORT": {
                         "Chat": 49151,
                         "Remote": 49150,
                         "Audio": 49149,
                         "File": 49152
                     },
                     "Screen": {
                         "alwaysOnTop": true,
                         "width": 232,
                         "yc": 0,
                         "xc": 0,
                         "height": 958
                     },
                     "Style": {
                         "bgColor": 402653183,
                         "fontType1": "JetBrains Mono NL Medium",
                         "fontType2": "JetBrains Mono NL ExtraLight",
                         "fontSize1": 12,
                         "fontColor2": -16737895,
                         "fontColor1": -3355444,
                         "fontSize2": 17,
                         "opacity": 1
                     },
                     "Paths": {"sensorDataFilePath": "C:\\\\\\\\ProgramData\\\\\\\\sensorData.json"},
                     "ProgressBar": {
                         "progressBarHeight": 10,
                         "progressBarWidth": 214,
                         "progressBarBackgroundColor": 402653183,
                         "progressBarForegroundColor": -16737895
                     },
                     "Disk": {
                         "diskName": "C,D,F",
                         "showAllDisk": true
                     },
                     "Chart": {"chartWidth": 214}
                 }""";

    static File getSettingsFile() {
        String jarDir;
        try {
            jarDir = new File(SettingsLogger.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (URISyntaxException e) {
            jarDir = System.getProperty("user.dir");
        }
        File dir = new File(jarDir, "config");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, SETTINGS_FILE);
    }


    public static void saveSettings(String fontType1, int fontSize1, Color fontColor1, String fontType2, int fontSize2, Color fontColor2, Double opacity, Color bgColor) {
        JSONObject settings = loadSettings();

        JSONObject style = new JSONObject();
        style.put("fontType1", fontType1);
        style.put("fontSize1", fontSize1);
        if (fontColor1 == null) {
            fontColor1 = new Color(settings.getJSONObject("Style").optInt("fontColor1"));
        }
        if (fontColor2 == null) {
            fontColor2 = new Color(settings.getJSONObject("Style").optInt("fontColor2"));
        }
        style.put("fontColor1", fontColor1.getRGB());
        style.put("fontType2", fontType2);
        style.put("fontSize2", fontSize2);
        style.put("fontColor2", fontColor2.getRGB());
        style.put("opacity", opacity);
        style.put("bgColor", bgColor.getRGB());

        settings.put("Style", style);

        File settingsFile = getSettingsFile();
        try (FileWriter writer = new FileWriter(settingsFile)) {
            writer.write(settings.toString(4));
        } catch (IOException e) {
            System.out.println("Đã xảy ra lỗi khi lưu cài đặt.");
        }
    }

    public static JSONObject loadSettings() {
        File settingsFile = getSettingsFile();
        if (!settingsFile.exists() || settingsFile.length() == 0 || !settingsFile.canRead()) {
            try (FileWriter fileWriter = new FileWriter(settingsFile)) {
                fileWriter.write(DEFAULT_SETTINGS);
                System.out.println("Tệp tin cài đặt đã được tạo.");
            } catch (IOException e) {
                System.out.println("Đã xảy ra lỗi khi tạo tệp tin cài đặt.");
            }
        }
        JSONObject settings;
        try (FileReader reader = new FileReader(settingsFile)) {
            settings = new JSONObject(new JSONTokener(reader));
        } catch (IOException e) {
            System.out.println("Đã xảy ra lỗi khi đọc tệp tin cài đặt.");
            settings = new JSONObject(DEFAULT_SETTINGS);
        }
        return settings;
    }

}
