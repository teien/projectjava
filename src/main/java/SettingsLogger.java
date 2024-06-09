import java.awt.*;
import java.io.*;

import org.json.JSONObject;
import org.json.JSONTokener;

public class SettingsLogger {
    private static final String SETTINGS_FILE = "settings.json";
    private static final String DEFAULT_SETTINGS = """
            {
              "Style": {
                 "bgColor": 203451456,
                 "fontType1": "JetBrains Mono",
                 "fontType2": "Candara Light",
                 "fontSize1": 12,
                 "fontColor2": -52480,
                 "fontColor1": -1118482,
                 "fontSize2": 14,
                 "opacity": 1
              },
              "Screen": {
                 "xc": 0,
                 "yc": 0,
                 "width": 250,
                 "height": 900
              },
              "Show/Hide": {
                      "showCPUTitle": true,
                      "showCpuName": true,
                      "showCpuUsage": true,
                      "showCpuTemp": true,
              
                      "showGpuTitle": true,
                      "showGpuName": true,
                      "showGpuUsage": true,
                      "showGpuTemp": true,
              
                      "showRamTitle": true,
                      "showRamInUse": true,
                      "showRamFree": true,
                     "showRamTotal": true,
              
                      "showDiskTitle": true,
                      "showDiskName": true,
                      "showDiskUsage": true,
                      "showDiskFree": true,
                      "showDiskTotal": true,
              
                      "showNetTitle": true,
                      "showNetName": true,
                      "showNetUsage": true,
                      "showNetTotal": true,
                      "showNetSpeed": true,
              
                      "showTimeTitle": true,
                      "showTime": true,
                      "showDate": true,
              
                      "showWeatherTitle": true,
                      "showWeather": true,
                      "showWeatherIcon": true,
                      "showWeatherTemp": true,
                      "showWeatherCity": true
                  }
              "": {
              }
            }""";

    private static File getSettingsFile() {
        String jarDir = new File(SettingsLogger.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        File dir = new File(jarDir, "config");

        if (!dir.exists()) {
            dir.mkdirs();
        }
        File settingsFile = new File(dir, SETTINGS_FILE);
        System.out.println("File settings được lưu tại: " + settingsFile.getAbsolutePath());
        return settingsFile;
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
            e.printStackTrace();
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
                e.printStackTrace();
            }
        }
        try (FileReader reader = new FileReader(settingsFile)) {
            return new JSONObject(new JSONTokener(reader));
        } catch (IOException e) {
            e.printStackTrace();
            return new JSONObject(DEFAULT_SETTINGS);
        }
    }
}
