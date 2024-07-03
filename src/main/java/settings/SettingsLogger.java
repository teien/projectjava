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
    "diskName": "C:",
    "Show/Hide": {
        "CPU": {
            "showCPUTitle": true,
            "showCpuName": true,
            "showCpuTemp": true,
            "showCpuUsage": true
        },
        "DATETIME": {
            "showDate": true,
            "showTime": true,
            "showTimeTitle": true
        },
        "GPU": {
            "showGpuName": true,
            "showGpuTemp": true,
            "showGpuUsage": true,
            "showGPUTitle": true
        },
        "KERNEL": {
            "showKernel": true
        },
        "NETWORK": {
            "showNetworkDownloadMonitor": true,
            "showNetworkDownloadSpeed": true,
            "showNetworkDownloadTotal": true,
            "showNetworkIP": true,
            "showNetworkUploadMonitor": true,
            "showNetworkUploadSpeed": true,
            "showNetworkUploadTotal": true,
            "showNETWORKTitle": true
        },
        "PROCESS": {
            "showProcess": true,
            "showProcessTitle": true
        },
        "RAM": {
            "showRamFree": true,
            "showRamInUse": true,
            "showRamTotal": true,
            "showRAMTitle": true
        },
        "STORAGE": {
            "showSsdFree": true,
            "showSsdName": true,
            "showSsdTotal": true,
            "showSsdUsed": true,
            "showSSDTitle": true
        },
        "SYSTEM": {
            "showSYSTEMTitle": true,
            "showUptime": true
        },
        "WEATHER": {
            "showWeather": true
        }
    },
    "Screen": {
        "alwaysOnTop": false,
        "height": 940,
        "width": 230,
        "xc": 0,
        "yc": 0
    },
    "Style": {
        "bgColor": 1309486352,
        "fontColor1": -16724788,
        "fontColor2": -65536,
        "fontSize1": 12,
        "fontSize2": 14,
        "fontType1": "JetBrains Mono Light",
        "fontType2": "JetBrains Mono NL ExtraLight",
        "opacity": 1
    },
    "Paths": {
        "sensorDataFilePath" : "C:\\\\\\\\ProgramData\\\\\\\\sensorData.json"
    },
    "Chart": {
        "chartWidth": 214
    },
    "PORT": {
        "Remote": 49150,
        "File": 49152,
        "Audio": 49149,
        "Chat" : 49151 \s
        }
}""";

    private static File getSettingsFile() {
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
