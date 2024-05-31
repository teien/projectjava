import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServiceManager {

    private static final String SERVICE_NAME = "HardwareMonitorService";
    private static final String SENSOR_DATA_FILE_PATH = "C:\\sensorData.json";
    private static final String CPU = "CPU";
    private static final String GPU = "GPU";
    private static final String CPU_PACKAGE = "CPU Package";
    private static final String GPU_CORE = "GPU Core";
    private static final String TEMPERATURE = "Temperature";
    private static final String USAGE = "Usage";

    private static JSONArray sensorDataArray;

    public static void getHardwareInfo() {
        if (sensorDataArray == null) {
            startAndEnsureServiceRunning();
        }
    }

    public static double getCpuTemperature() {
        getHardwareInfo();
        return getSensorValue(CPU, CPU_PACKAGE, TEMPERATURE);
    }

    public static double getGpuTemperature() {
        getHardwareInfo();
        return getSensorValue(GPU, GPU_CORE, TEMPERATURE);
    }

    public static double getGpuUsage() {
        getHardwareInfo();
        return getSensorValue(GPU, GPU_CORE, USAGE);
    }
    public static String getCpuName() {
        return getSensorName(CPU, CPU_PACKAGE);
    }
    public static String getGpuName() {
        return getSensorName(GPU, GPU_CORE);
    }
    private static String getSensorName(String hardwareType, String sensorName) {
        if (sensorDataArray != null) {
            for (Object sensorDataObj : sensorDataArray) {
                JSONObject sensorData = (JSONObject) sensorDataObj;
                String hwType = (String) sensorData.get("HardwareType");
                String sName = (String) sensorData.get("SensorName");
                String hwName = (String) sensorData.get("HardwareName");

                if (hardwareType.equals(hwType) && sensorName.equals(sName)) {
                    return hwName;
                }
            }
        }
        return "Unknown";
    }


    private static double getSensorValue(String hardwareType, String sensorName, String dataType) {
        if (sensorDataArray != null) {
            for (Object sensorDataObj : sensorDataArray) {
                JSONObject sensorData = (JSONObject) sensorDataObj;
                String hwType = (String) sensorData.get("HardwareType");
                String sName = (String) sensorData.get("SensorName");
                Double value = (Double) sensorData.get("Value");
                String dType = (String) sensorData.get("DataType");
                if (hardwareType.equals(hwType) && sensorName.equals(sName) && dataType.equals(dType)) {
                    return value != null ? value : 0.0;
                }
            }
        }
        return 0.0;
    }

    private static void startAndEnsureServiceRunning() {
        sensorDataArray = readSensorData();
        if (!isServiceRunning()) {
            startService();
            try {
                Thread.sleep(5000); // Wait for the service to start
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> executeCommand("sc stop " + SERVICE_NAME)));
    }

    private static boolean isServiceRunning() {
        String commandOutput = executeCommand("sc query " + ServiceManager.SERVICE_NAME);
        return commandOutput.contains("STATE              : 4  RUNNING");
    }

    private static void startService() {
        executeCommand("sc start " + ServiceManager.SERVICE_NAME);
    }

    private static JSONArray readSensorData() {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(ServiceManager.SENSOR_DATA_FILE_PATH)) {
            return (JSONArray) parser.parse(reader);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("cmd.exe", "/c", command);

        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return output.toString();
    }
}
