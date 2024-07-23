package system;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import settings.SettingsLogger;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServiceManager {
    static org.json.JSONObject settings = SettingsLogger.loadSettings();
    private static final String SENSOR_DATA_FILE_PATH = settings.getJSONObject("Paths").getString("sensorDataFilePath");
    private static final String SERVICE_NAME = "HardwareMonitorService";
    private static final String CPU_PACKAGE = "CPU Package";
    private static final String GPU_CORE = "GPU Core";
    private static final String iGPU ="D3D 3D";
    private static final String TEMPERATURE = "Temperature";
    private static final String USAGE = "Load";

    private static JSONArray lastSensorDataArray = null;

    public record HwInfo(Double cpuTemperature, Double gpuTemperature, Double gpuUsage, Double cpuUsage, Double iGpuUsage , String cpuName,
                         String gpuName) {



        public static HwInfo getHwInfo() {
            new SwingWorker<>() {
                @Override
                protected HwInfo doInBackground() {
                    startAndEnsureServiceRunning();
                    return null;
                }
            }.execute();

            JSONArray sensorDataArray = readSensorData();

            if (sensorDataArray == null) {
                sensorDataArray = lastSensorDataArray;
            } else {
                lastSensorDataArray = sensorDataArray;
            }

            if (sensorDataArray == null) {
                return new HwInfo(0.0, 0.0, 0.0, 0.0, 0.0, "", "" );
            }

            double cpuTemp = 0.0;
            double gpuTemp = 0.0;
            double gpuUsage = 0.0;
            double cpuUsage = 0.0;
            String cpuName = "";
            String gpuName = "";
            double iGpuUsage = 0.0;

            for (Object sensorDataObj : sensorDataArray) {
                JSONObject sensorData = (JSONObject) sensorDataObj;

                String hardwareType = (String) sensorData.get("HardwareType");
                String hardwareName = (String) sensorData.get("HardwareName");
                String sensorName = (String) sensorData.get("SensorName");
                String dataType = (String) sensorData.get("DataType");
                Object valueObj = sensorData.get("Value");

                if (valueObj instanceof Number) {
                    double value = ((Number) valueObj).doubleValue();

                    if (TEMPERATURE.equals(dataType)) {
                        if (CPU_PACKAGE.equals(sensorName)) {
                            cpuTemp = value;
                            cpuName = hardwareName;
                        } else if (GPU_CORE.equals(sensorName)) {
                            gpuTemp = value;
                            gpuName = hardwareName;

                        }
                    } else if (USAGE.equals(dataType)) {
                         if ("GPU Core".equals(sensorName)) {
                            gpuUsage = value;
                        } else if (iGPU.equals(sensorName)) {
                            iGpuUsage = value;
                        }
                    }
                }
            }

            return new HwInfo(cpuTemp, gpuTemp, gpuUsage, cpuUsage,iGpuUsage, gpuName,  cpuName);
        }
    }
    private static void startAndEnsureServiceRunning() {
        if (!isServiceRunning()) {
            startService();
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
            System.out.println("Error reading sensor data: " + e.getMessage());
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
            System.out.println("Error executing command: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        return output.toString();
    }
}
