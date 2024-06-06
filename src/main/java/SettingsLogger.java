import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SettingsLogger {
    private static final String SETTINGS_FILE = "settings.json";

    public static void saveSettings(String fontType, int fontSize, Color fontColor, Double opacity, Color bgColor) {
        JSONObject settings = new JSONObject();
        settings.put("fontType", fontType != null ? fontType : Font.SANS_SERIF);
        settings.put("fontSize", fontSize != 0 ? fontSize : 14);
        settings.put("fontColor", fontColor != null ? fontColor.getRGB() : Color.BLACK.getRGB());
        settings.put("opacity", opacity != null ? opacity : 1.0);
        settings.put("bgColor", bgColor != null ? bgColor.getRGB() :    Color.WHITE.getRGB());

        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            writer.write(settings.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static JSONObject loadSettings() {
        try (FileReader reader = new FileReader(SETTINGS_FILE)) {
            System.out.println("Loading settings from " + SETTINGS_FILE);
            return new JSONObject(new JSONTokener(reader));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
