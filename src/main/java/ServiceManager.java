import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServiceManager {

    public static void main(String[] args) {
        startService("HardwareMonitorService");
        // Đợi vài giây để dịch vụ khởi động và tạo tệp sensorData.json
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        readSensorData("C:\\MyApp\\sensorData.json");
    }

    public static void startService(String serviceName) {
        executeCommand("sc start " + serviceName);
    }

    public static void readSensorData(String filePath) {
        try {
            BufferedReader reader = new BufferedReader(new java.io.FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeCommand(String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("cmd.exe", "/c", command);

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
