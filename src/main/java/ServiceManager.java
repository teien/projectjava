import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServiceManager {
    private static final String SENSOR_DATA_FILE_PATH = "C:\\sensorData.json";
    private static final String SERVICE_NAME = "HardwareMonitorService";
    private static final String CPU_PACKAGE = "CPU Package";
    private static final String GPU_CORE = "GPU Core";
    private static final String TEMPERATURE = "Temperature";
    private static final String USAGE = "Usage";


    public record HwInfo(Double cpuTemperature, Double gpuTemperature, Double gpuUsage, Double cpuUsage, String cpuName,
                         String gpuName) {

        public static HwInfo getHwInfo() {
            Double cpuTemp = 0.0;
            Double gpuTemp = 0.0;
            Double gpuUsage = 0.0;
            Double cpuUsage = 0.0;
            String cpuName = "";
            String gpuName = "";
            startAndEnsureServiceRunning();
            JSONArray sensorDataArray = readSensorData();
            if (sensorDataArray != null) {
                for (Object sensorDataObj : sensorDataArray) {
                    JSONObject sensorData = (JSONObject) sensorDataObj;

                    String hardwareType = (String) sensorData.get("HardwareType");
                    String hardwareName = (String) sensorData.get("HardwareName");
                    String sensorName = (String) sensorData.get("SensorName");
                    String dataType = (String) sensorData.get("DataType");
                    Object valueObj = sensorData.get("Value");

                    Double value = null;
                    if (valueObj instanceof Number) {
                        value = ((Number) valueObj).doubleValue();
                    }

                    if (TEMPERATURE.equals(dataType)) {
                        if (CPU_PACKAGE.equals(sensorName)) {
                            cpuTemp = value;
                            cpuName = hardwareName;
                        } else if (GPU_CORE.equals(sensorName)) {
                            gpuTemp = value;
                            gpuName = hardwareName;
                        }
                    } else if (USAGE.equals(dataType)) {
                        if ("CPU".equals(hardwareType)) {
                            cpuUsage = value;
                        } else if ("GPU".equals(hardwareType)) {
                            gpuUsage = value;
                        }
                    }
                }
            }

            return new HwInfo(cpuTemp, gpuTemp, gpuUsage, cpuUsage, cpuName, gpuName);
        }
    }





    private static void startAndEnsureServiceRunning() {
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
