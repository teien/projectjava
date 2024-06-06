import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SettingsLogger {
    private static final String SETTINGS_FILE = "settings.json";

    public static void saveSettings(String fontType1, int fontSize1, Color fontColor1, String fontType2, int fontSize2, Color fontColor2,Double opacity, Color bgColor) {
        JSONObject settings = new JSONObject();
        settings.put("fontType1", fontType1 != null ? fontType1 : Font.SANS_SERIF);
        settings.put("fontSize1", fontSize1 != 0 ? fontSize1 : 14);
        settings.put("fontColor1", fontColor1 != null ? fontColor1.getRGB() : Color.BLACK.getRGB());
        settings.put("opacity", opacity != null ? opacity : 1.0);
        settings.put("bgColor", bgColor != null ? bgColor.getRGB() :    Color.WHITE.getRGB());
        settings.put("fontType2", fontType2 != null ? fontType2 : Font.SANS_SERIF);
        settings.put("fontSize2", fontSize2 != 0 ? fontSize2 : 14);
        settings.put("fontColor2", fontColor2 != null ? fontColor2.getRGB() : Color.BLACK.getRGB());

        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            writer.write(settings.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static JSONObject loadSettings() {
        try (FileReader reader = new FileReader(SETTINGS_FILE)) {
            return new JSONObject(new JSONTokener(reader));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
