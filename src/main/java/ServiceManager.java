import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServiceManager {
    public static double getCpuTemperature() {
        Double cpuTemperature = null;
        if (!isServiceRunning("HardwareMonitorService")) {
            startService("HardwareMonitorService");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executeCommand("sc stop HardwareMonitorService");
        }));

        JSONArray sensorDataArray = readSensorData("C:\\sensorData.json");
        if (sensorDataArray != null) {
            for (Object sensorDataObj : sensorDataArray) {
                JSONObject sensorData = (JSONObject) sensorDataObj;
                String hardwareType = (String) sensorData.get("HardwareType");
                String sensorName = (String) sensorData.get("SensorName");
                Double value = (Double) sensorData.get("Value");

                if ("CPU".equals(hardwareType) && "CPU Package".equals(sensorName)) {
                    cpuTemperature = value;
                    break;
                }
            }
        }

        return cpuTemperature;
    }

    public static boolean isServiceRunning(String serviceName) {
        String commandOutput = executeCommand("sc query " + serviceName);
        return commandOutput.contains("STATE              : 4  RUNNING");
    }

    public static void startService(String serviceName) {
        executeCommand("sc start " + serviceName);
    }


    private static JSONArray readSensorData(String filePath) {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(filePath)) {
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
        }
        return output.toString();
    }
}